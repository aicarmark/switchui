#! /bin/env /vobs/atso_tools/build/inttool/perl

use strict;
use warnings;
use File::Basename;
use HTTP::Request;
use HTTP::Request::Common qw(POST);
use LWP;
use LWP::UserAgent;
use Getopt::Long;

# constant array for calculating the crc
my @fax_util_gentab = (
                       0x0000, 0x1081, 0x2102, 0x3183, 0x4204, 0x5285, 0x6306, 0x7387,
                       0x8408, 0x9489, 0xa50a, 0xb58b, 0xc60c, 0xd68d, 0xe70e, 0xf78f
                      );

my $debug   = 1;
my $readelf = "/vobs/atso_tools/util/bin/readelf";

# Get Data from config file
sub Get_Datetime_from_pvc
{
    my $filename = shift;
    my %results;

    # open product_version.c to get variables for contructing the crc
    open INPUT, $filename or die "can't open $filename: $!";
    while ( <INPUT> )
    {
        print "Looking at: $_ " if ( $debug > 1 );
        if ( /bldinfo_username\[8\+1\]\s+= "(\w+)"/ )
        {
            $results{ username } = $1;
        }
        if ( /bldinfo_datetime\[\]\s+= "([\w\s\-:]+)"/ )
        {
            $results{ datetime } = $1;
        }
        if ( /bldinfo_baselabel\[.*?\]\s+= "([A-Za-z0-9_\.-]+)"/ )
        {
            $results{ baselabel } = $1;
        }
        if ( /product_version\[4\+34\+1\]\s+= "\@\(\#\)([A-Za-z0-9_\.-]+)"/ )
        {
            $results{ product_version } = $1;
        }
    }
    close INPUT;
    if ( $debug )
    {
        print "pvc Read Username: $results{username}\n";
        print "pvc Read Date $results{datetime}\n";
        print "pvc Read Baselabel $results{baselabel}\n";
        print "pvc Read Product_version $results{product_version}\n";
    }
    return %results;
} # End Get_Datetime_from_pvc


#Get data from Elf File.
sub Get_Datetime_from_ELF
{
    my $filename = shift;

    my ( %results, %address, %lengths );
    my ( $gz );

    # get address of bldinfo_user and bldinfo_datetime
    # first check if elf file is compressed
    if ( $filename =~ /\.gz$/ )
    {
        print "Need to unzip elf file\n" if $debug;
        system( "/bin/gunzip $filename" );
        $filename =~ s/\.gz$//;
        die "unable to unzip $filename\n" unless ( -e $filename );
        $gz = 1;
    }
    open ELF_SYMBOLS, "$readelf --syms $filename | egrep 'bldinfo|product_version' |"
        or die "can't open $filename: $!";

    #open ELF_SYMBOLS, "readelf --syms $filename | egrep 'bldinfo' |";
    while ( <ELF_SYMBOLS> )
    {
        if ( /(\w+)\s+(\d+) OBJECT\s+GLOBAL\s+\w+\s+\d+ bldinfo_username/ )
        {
            $address{ username } = $1;
            $lengths{ username } = $2;
            print "ELF Username address is $address{username}\n" if $debug;
        }
        elsif ( /(\w+)\s+(\d+) OBJECT\s+GLOBAL\s+\w+\s+\d+ bldinfo_datetime/ )
        {
            $address{ datetime } = $1;
            $lengths{ datetime } = $2;
            print "ELF Datetime address is $address{datetime}\n" if $debug;
        }
        elsif ( /(\w+)\s+(\d+) OBJECT\s+GLOBAL\s+\w+\s+\d+ bldinfo_baselabel/ )
        {
            $address{ baselabel } = $1;
            $lengths{ baselabel } = $2;
            print "ELF Baselabel address is $address{baselabel}\n" if $debug;
        }
        elsif ( /(\w+)\s+(\d+) OBJECT\s+GLOBAL\s+\w+\s+\d+ product_version$/ )
        {
            $address{ product_version } = $1;
            $lengths{ product_version } = $2;
            print "ELF Product_version address is $address{product_version}\n" if $debug;
        }
    }
    close ELF_SYMBOLS;

    # convert %address to decimal
    foreach my $name ( keys %address ) { $address{ $name } = &HexConv( $address{ $name } ); }

    # get base address of data section
    open ELF_SECTION, "$readelf -S $filename |" or die "can't open $filename: $!";
    my ( $temp, $base, $last_base, $last_offset );
    my $offset = 0;
    while ( <ELF_SECTION> )
    {
        if ( /\w+\s+(\w+) (\w+) \w+ \w{2}/ )
        {
            $temp = &HexConv( $1 );
            if ( $temp > $address{ username } )
            {
                $base   = $last_base;
                $offset = $last_offset;
            }
            else
            {
                $last_base   = $temp;
                $last_offset = &HexConv( $2 );
            }
        }
    }
    close ELF_SECTION;

    # convert the addresses to absolute seeks
    foreach my $name ( keys %address )
    {
        $address{ $name } = $offset + $address{ $name } - $base;
    }

    # open elf file in binary mode
    open ELF, $filename or die "can't open $filename: $!";
    binmode( ELF );

    # grab data from each address
    foreach my $name ( keys %address )
    {
        seek( ELF, $address{ $name }, 0 );
        read( ELF, $results{ $name }, $lengths{ $name } );
        $results{ $name } =~ s/\000//g;
        $results{ $name } =~ s/^\@\(\#\)//;
    }
    close ELF;
    if ( $debug )
    {
        foreach my $name ( keys %results )
        {
            print "ELF Read $name: $results{$name}\n";
        }
    }
    print "\n"                      if $debug;
    system( "/bin/gzip $filename" ) if $gz;
    return %results;
}

sub HexConv
{
    my $input = shift;
    my $output;

    if ( $input =~ /^0/ ) { $output = oct( "0x" . $input ); }
    else
    {
        $input =~ s/[G-Zg-z]//g;
        $output = hex( $input );
    }
    return $output;
}

#Get CRC from a given map file/
sub GetCRC
{
    my $results_ref = shift;
    my %results     = %$results_ref;

    # combine the 2 strings
    my $ptr = $results{ username } . $results{ datetime };

    # Print the string
    print "Combined string is $ptr\n";

    my $len = length( $ptr );
    my ( $crc, $c, $temp, $index );

    # initialize variables for calulating the crc
    $crc   = 0xffff;
    $index = 0;
    my @c = unpack( 'U*', $ptr );

    # crc calculation loop
    while ( $len )
    {
        $c    = $c[ $index++ ];
        $temp = ( ( $crc & 0xff ) ^ $c ) & 0x0f;
        $temp = $fax_util_gentab[ $temp ];
        $crc  = ( $crc >> 4 ) ^ $temp;
        $c >>= 4;
        $temp = ( ( $crc & 0xff ) ^ $c ) & 0x0f;
        $temp = $fax_util_gentab[ $temp ];
        $crc  = ( $crc >> 4 ) ^ $temp;
        $crc &= 0xffff;
        $len--;
    }
    $crc = ( ~$crc & 0xffff );

my $crc_string  = '';

    $crc_string = sprintf( "CRC Result Is: %04X", $crc );

    print( "$crc_string\n\n" );

    return ( $crc );
}

sub GetFiles
{
    my $dir_name  = shift;
    my $extension = shift;

    opendir( DIR, $dir_name ) or die "can't opendir $dir_name: $!";

    # get a list of all files in the directory
    # foreach file
    my @file_list = ();
    while ( defined( my $file = readdir( DIR ) ) )
    {
        next if $file =~ /^\..*$/;    # skip anything starting with .
        push(@file_list, $file)
            if ( $file =~ /$extension$/ );
    }
    closedir DIR;
    return join(",", @file_list);
}

#If no elf file supplied
sub PrintUsage
{
    print "Usage: Map_Upload.pl <options>\n";
    print "  Version:  1.6 \n";
    print "  Required:\n";
    print "    -elf_file=<file>         Phone Executable (may be zipped)\n";
    print "    -baselabel=<string> AND  Baselabel string and crc value may be\n";
    print "       -crc=<hex_value>        supplied as alternative to elf on some platforms\n";
    print "    -official OR      APR server must be told whether\n";
    print "       -testbuild       the build is Official or a Testbuild\n";
    print "  Optional\n";
    print "    -prod_ver=<file>  product_version.c file from the build\n";
    print "    -map_file=<file>  map file from the build, used for debugging\n";
    print "    -hash_file=<file> AJAR specific hash file from the build, used for debugging\n";
    print "    -checkCRC         calculate CRC and exit\n";
    print "    \n";
    print "    \n";
    exit (0);
}

# start MAIN

# initialize the options
my ( $product_version_c, $mapfile, $elffile, $hashfile ) = ( "", "", "", "" );
my $help      = 0;
my $official  = 0;
my $testbuild = 0;
my ( $get_results, $baselabel, $checkCRC, $crcstr );

#call GetOption to assign the values
$get_results = GetOptions(
                          "prod_ver=s"  => \$product_version_c,
                          "map_file=s"  => \$mapfile,
                          "elf_file=s"  => \$elffile,
                          "hash_file=s" => \$hashfile,
                          "crc=s"       => \$crcstr,              #crc entered as string
                          "baselabel=s" => \$baselabel,
                          "official"    => \$official,
                          "testbuild"   => \$testbuild,
                          "checkCRC"    => \$checkCRC,
                          "debug"       => \$debug,
                          "help"        => \$help
                         );

# Either the elf file or the crc must be defined
PrintUsage()  unless ( defined $elffile  or defined $crcstr );
PrintUsage()  unless ( defined $official or defined $testbuild );

my $crc;
if ( $crcstr )
{
    $crc = hex( $crcstr );
}

my %elf_results = &Get_Datetime_from_ELF( $elffile )           if $elffile;
my %pvc_results = &Get_Datetime_from_pvc( $product_version_c ) if $product_version_c;
my %params;

&PrintUsage if ( $help );

#product verions.c data matched elf file data
if ( $elffile && $product_version_c )
{
    if (   ( $elf_results{ datetime } eq $pvc_results{ datetime } )
        && ( $elf_results{ username }  eq $pvc_results{ username } )
        && ( $elf_results{ baselabel } eq $pvc_results{ baselabel } ) )
    {
        print "\n**Match** Data in product_version.c matches elf file.\n\n";
    }
    else
    {
        print "\n**WARNING** Data in product_version.c doesn't match elf file.\n\n";
        print "     product_version.c : _$pvc_results{datetime}_ \n";
        print "              elf file : _$elf_results{datetime}_ \n";
        print "     product_version.c : _$pvc_results{username}_ \n";
        print "              elf file : _$elf_results{username}_ \n";
        print "     product_version.c : _$pvc_results{baselabel}_ \n";
        print "              elf file : _$elf_results{baselabel}_ \n";
        print "  Uploading map based on datetime from elf file.\n";
    }
}
elsif ( !$elffile && !$product_version_c && $baselabel )
{
    $params{ baselabel } = $baselabel;
}
if (   !$crc
    && exists( $elf_results{ datetime } )
    && exists( $elf_results{ username } )
    && exists( $elf_results{ baselabel } )
    && exists( $elf_results{ product_version } ) )
{
    $crc = &GetCRC( \%elf_results );
}
my $filename  = '';
my $hash_file = '';
my $error     = 0;
if ( $crc )
{
    if ( $checkCRC )    #If passed in with a param to get CRC.
    {
        #printf "\nCRC is %04X\n\n", $crc;
    }
    else
    {
        if ( $mapfile )
        {
            $filename = basename( $mapfile );
            $filename = sprintf( "%04X.%s", $crc, $filename );
        }
        elsif ( $hashfile )
        {
            $hash_file = basename( $hashfile );
            $hash_file = sprintf( "%04X.%s", $crc, $hash_file );
        }
        my $ua = LWP::UserAgent->new;
        $ua->credentials( "apr.pcs.mot.com:80", "APR", "java", "pdf" );

        # url to upload to
        my $URL     = "http://dl2k407.am.mot.com/largeupload.php";
        my $postURL = $URL . "?";

        # build params array
        foreach my $name ( keys %elf_results ) { $params{ $name } = $elf_results{ $name }; }

        foreach my $key ( keys( %ENV ) ) { $params{ user } = $ENV{ $key } if $key =~ /^user/i; }

        $params{ official }  = $official;
        $params{ testbuild } = $testbuild;
        $params{ crc }       = $crc;
        $params{ crc_hex }   = sprintf( "%04X", $crc );
        $params{ debug }     = 1 if ( $debug );
        my ( @upload_file1, @upload_file2 );
        my %secondparams;
        if ( $mapfile )
        {
            $upload_file1[ 0 ]       = $mapfile;
            $upload_file1[ 1 ]       = $filename;
            $secondparams{ 'fname' } = \@upload_file1;
        }
        elsif ( $hashfile )
        {
            $upload_file2[ 0 ]        = $hashfile;
            $upload_file2[ 1 ]        = $hash_file;
            $secondparams{ 'hfname' } = \@upload_file2;
        }
        foreach my $key ( keys( %params ) )    #Fill in parameters for second submit.
        {
            $secondparams{ $key } = $params{ $key };
        }

        # build a command for POST
        my $request = POST $postURL,
            Content_Type => 'multipart/form-data',
            Content      => \%params;

        # Execute post request
        my $res = $ua->request( $request );

        #If result is failure
        if ( !$res->is_success )
        {
            print $res->status_line, "\n";
            exit 1;
        }
        my $result = $res->content;

        #Set swverID parameter for second submit
        if ( $result =~ /(\d+)/ )
        {
            $secondparams{ 'swverID' } = $1;
        }
        if ( $mapfile || $hashfile )
        {

            #If map or hash file was provided and wasn't uploaded yet, upload.
            #If map already uploaded, print out error message.
            if ( $result =~ /Error/ )
            {
                $error = 1;
                if ( $result =~ /\*\*Error\*\*: Map/ )
                {
                    print "**Error**: Map already uploaded with CRC " . $crc . "\n";
                }
            }
            else
            {
                $secondparams{ 'fnamecrc' } = $filename  if ( $mapfile );
                $secondparams{ 'fnamecrc' } = $hash_file if ( $hashfile );
                my $request2 = POST $postURL,
                    Content_Type => 'multipart/form-data',
                    Content      => \%secondparams;

                my $res2 = $ua->request( $request2 );
                print $res2->content;
                if ( !$res2->is_success )
                {
                    print $res2->status_line, "\n";
                    $error = 1;
                }
            }
        }
    }
    exit $error;
}
else
{
    print "\n  ERROR: Elf file data missing\n";
    print "    Looking for Username found $elf_results{username}\n";
    print "    Looking for Datetime found $elf_results{datetime}\n";
    print "    Looking for Base_label found $elf_results{baselabel}\n";
    print "    Looking for Product_version found $elf_results{product_version}\n";
    exit 1;
}
