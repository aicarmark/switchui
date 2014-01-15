#!/usr/bin/perl

# print "This script will update the input .xml file, appending the";
# print "remote name and url specified, and swapping the default";
# print "revision for the newly added remote.  It also requires a new";
# print "name for the default revision.";
# print "$0 usage:\n";
# print "$0 <preferences_file> <operation type>

$debug = 0;
$enabled_changed=0;
$apr_enabled_changed=0;
$printed=0;

#$/ = '>';                    # Record end is > newline
while ( <> )  {              # fetch a paragraph

  # if we reach the end of the file and these lines aren't there
  #   put them in!
  if  ( ( m/<.*APRPrefsEnableTestBench.*\/string>/sm ) | ( m/<\/map.*\>/sm ) ) {

      if ( $enabled_changed == 0 ) {
        print "<string name=\"APRPrefsEnableTestBench\">";
        print $ARGV[0];
        print "</string>\n";
        $enabled_changed=1;

        if ( m/<\/map>/sm ) {
            $printed=0;
         } else {
            $printed=1;
         }
     }
  }

  # if we reach the end of the file and these lines aren't there
  #   put them in!
  if ( ( m/<.*APRPrefsEnabledState.*>/sm ) | ( m/<\/map>/sm ) ) {

      if ( $apr_enabled_changed == 0 ) {
         print "<boolean name=\"APRPrefsEnabledState\" value=\"";
         print "true";
         print "\" />\n";
         $apr_enabled_changed=1;
      
         if ( m/<\/map>/sm ) {
            $printed=0;
         } else {
            $printed=1;
         }
      }
  }

  if ( $printed == 0 ) {
     print;
  }

  $printed = 0;
}




