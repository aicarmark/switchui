<?php

//
// Sholes UMTS panic website version 1.0
// 2009. 12. 11. 
// Paul Lee
//

// name of php
$board_file="milestone.php";

// account
//$account = "aprlogs";

// root path for the account
$root_path = "/var/www";

// url for the root path
$root_url = "";

// directory for the php data
$board_directory="board";

// path of the board directory
$board_path = "$root_path/$board_directory";

// header file
$head_file="$board_directory/head.html";

// tail file
$tail_file="$board_directory/tail.html";

// path of the status file for software verssion and build CRC
$sw_status_file="$board_path/sw_status.txt";

// path of the panic name file
$panic_name_file="$board_path/panic_name.txt";

// URL for CR in DDTS
$ddts_url = "http://wsd-ddts.pcs.mot.com/ddts_auth/ddts_main?submit=View+Record&LastForm=DumpBug&bug_id=";

$td_color = "#E9EDFF";
$th_color = "#B0B9E2";
$table_color = "#1E2A63";

// Define constants
define("UNKNOWN_CRASH_TYPE_ID", 		"A0080500");
define("KERNEL_CRASH_ID", 		"A0080520");
define("MODEM_CRASH_ID", 		"A0080501");
define("LINUX_USER_SPACE_CRASH_ID", 	"A0080523");
define("JAVA_APP_CRASH_ID", 		"A0080524");
define("POWER_CUT_ID", 			"A0080525");
define("AP_WATCHDOG_ID", 		"A0080526");
define("CPCAP_WATCHDOG_ID", 		"A0080527");
define("KERNEL_APANIC_ND_ID", 		"A0080528");
define("JAVA_APP_CRASH_ANR_ID", 		"A0080529");


/////////////////////////////////////////
// Sub routine

// read a file and print as html
function ReadFilePrint($filename)
{
	$contents=file($filename);
	$n=count($contents);
	for ($i=0; $i<$n; $i=$i+1)
	{
		echo("$contents[$i]");
	}
}

function debug_print( $debug_string )
{
echo "<font color=red>$debug_string</font><br>";
}

/*
function table_head_linux_user_space_crash()
{
	global $table_color, $th_color;
echo "\n\n<!-- Start of table_head() -->\n";
echo("
<table width=550 cellspacing=0 cellpadding=0 border=0>
<tr><td bgcolor=\"$table_color\" width=550>
<table width=550 cellspacing=1 cellpadding=2 border=0>
<tr>
<th width=50 bgcolor=\"$th_color\" align=center><font size=2>Process name</th>
<th width=40 bgcolor=\"$th_color\" align=center><font size=2>Program counter</th>
<th width=260 bgcolor=\"$th_color\" align=center><font size=2>Barcode</th>
<th width=80 bgcolor=\"$th_color\" align=center><font size=2>Time</th>
<th width=80 bgcolor=\"$th_color\" align=center><font size=2>Link</th>
<th width=40 bgcolor=\"$th_color\" align=center><font size=2>Analysis</th>
</tr>
");
echo "\n<!-- End of table_head() -->\n\n";
}
*/

function table_head_show_all_software()
{
	global $table_color, $th_color, $sort_order;

	$next_order = ($sort_order +1)%2;
	//debug_print( "Sort order: $sort_order" );

echo "\n\n<!-- Start of table_head() -->\n";
echo("
<table width=600 cellspacing=0 cellpadding=0 border=0>
<tr><td bgcolor=\"$table_color\" width=600>
<table width=600 cellspacing=1 cellpadding=2 border=0>
<tr>
<th bgcolor=\"$th_color\" align=center><font size=2>
<a href=\"$board_file?job=all&sort=$next_order\">Software version</a></font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>Build CRC</font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>Software status</font></th>
</tr>
");
echo "\n<!-- End of table_head() -->\n\n";
}

function table_head_java( $grp )
{
	global $table_color, $th_color;
	global $board_file, $sw_version, $build_crc, $sort_order;

	$next_order = ($sort_order +1)%2;

echo "\n\n<!-- Start of table_head() -->\n";
echo("
<table width=800 cellspacing=0 cellpadding=0 border=0>
<tr><td bgcolor=\"$table_color\" width=800>
<table width=800 cellspacing=1 cellpadding=2 border=0>
<tr>
<th bgcolor=\"$th_color\" align=center><font size=2>
<a href=\"$board_file?job=single&sw=$sw_version&crc=$build_crc&grp=$grp&sort=$next_order&by=0\">App name</a></font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>
<a href=\"$board_file?job=single&sw=$sw_version&crc=$build_crc&grp=$grp&sort=$next_order&by=1\">Barcode</a></font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>
<a href=\"$board_file?job=single&sw=$sw_version&crc=$build_crc&grp=$grp&sort=$next_order&by=2\">Time</a></font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>Link</font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>Analysis</font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>Edit</font></th>
</tr>
");
echo "\n<!-- End of table_head() -->\n\n";
}

function table_head_linux_user_space_crash()
{
	global $table_color, $th_color;
	global $board_file, $sw_version, $build_crc, $sort_order;

	$next_order = ($sort_order +1)%2;

echo "\n\n<!-- Start of table_head() -->\n";
echo("
<table width=800 cellspacing=0 cellpadding=0 border=0>
<tr><td bgcolor=\"$table_color\" width=800>
<table width=800 cellspacing=1 cellpadding=2 border=0>
<tr>
<th bgcolor=\"$th_color\" align=center><font size=2>
<a href=\"$board_file?job=single&sw=$sw_version&crc=$build_crc&grp=ap&sort=$next_order&by=0\">Process name</a></font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>Program counter</font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>
<a href=\"$board_file?job=single&sw=$sw_version&crc=$build_crc&grp=ap&sort=$next_order&by=1\">Barcode</a></font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>
<a href=\"$board_file?job=single&sw=$sw_version&crc=$build_crc&grp=ap&sort=$next_order&by=2\">Time</a></font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>Link</font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>Analysis</font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>Edit</font></th>
</tr>
");
echo "\n<!-- End of table_head() -->\n\n";
}

function table_head_other_panic()
{
	global $table_color, $th_color;
	global $board_file, $sw_version, $build_crc, $sort_order;

	$next_order = ($sort_order +1)%2;

echo "\n\n<!-- Start of table_head() -->\n";
echo("
<table width=800 cellspacing=0 cellpadding=0 border=0>
<tr><td bgcolor=\"$table_color\" width=800>
<table width=800 cellspacing=1 cellpadding=2 border=0>
<tr>
<th bgcolor=\"$th_color\" align=center><font size=2>
<a href=\"$board_file?job=single&sw=$sw_version&crc=$build_crc&grp=bp&sort=$next_order&by=0\">Panic ID</a></font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>
<a href=\"$board_file?job=single&sw=$sw_version&crc=$build_crc&grp=bp&sort=$next_order&by=3\">Panic Name</a></font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>Panic Data</font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>
<a href=\"$board_file?job=single&sw=$sw_version&crc=$build_crc&grp=bp&sort=$next_order&by=1\">Barcode</a></font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>
<a href=\"$board_file?job=single&sw=$sw_version&crc=$build_crc&grp=bp&sort=$next_order&by=2\">Time</a></font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>Link</font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>Analysis</font></th>
<th bgcolor=\"$th_color\" align=center><font size=2>Edit</font></th>
</tr>
");
echo "\n<!-- End of table_head() -->\n\n";
}

function show_table_title( $table_title )
{
echo("
<center><table width=600 cellspacing=0 cellpadding=0 border=0>
<tr><td width=600>
<br><br>
<center><font size=2><b>$table_title</b></font></center>
<br>
</td></tr></table></center>\n
");
}

function show_center_message( $center_message )
{
echo("
<center><table width=600 cellspacing=0 cellpadding=0 border=0>
<tr><td width=600>
<center><font size=2>$center_message</font></center>
<br>
</td></tr></table></center>\n
");
}

function show_main_title( $main_title )
{
	global $board_file;
	
echo("
<center><table width=600 cellspacing=0 cellpadding=0 border=0>
<tr><td width=600>
<br><br>
<center><font size=6><b>
<a href=\"$board_file\">$main_title</a>
</b></font></center>
<br>
</td></tr></table></center>\n
");

}

function ShowRootDirectory()
{
	global $root_path;

	chdir($root_path);
	$root_dir = opendir($root_path);

	while (($entry = readdir($root_dir)) !== false) {
		if($entry != '.' && $entry != '..' && is_dir($entry))
		{ 
			$all_entry[] = $entry;
			//echo("<tr><td valign=\"top\"><img src=\"/icons/folder.gif\" alt=\"[DIR]\"></td><td><a href=\"$entry\">$entry</a></td></tr>\n");
		}
	}

	sort($all_entry,SORT_REGULAR);

	echo"<p><p><p><p><table>\n";
	foreach($all_entry as $key => $value)
	{
		echo("<tr><td valign=\"top\"><img src=\"/icons/folder.gif\" alt=\"[DIR]\"></td><td><a href=\"$value\">$value</a></td></tr>\n");
	}		
	echo"</table>\n";

	closedir($root_dir);
	chdir($root_path);
}

function get_all_software()
{
	global $account, $root_path, $root_url, $sw_version, $build_crc;
	global $all_sw, $all_crc, $all_status;

	chdir($root_path);
	$root_dir = opendir($root_path);

	while (($entry = readdir($root_dir)) !== false) {
		if($entry != '.' && $entry != '..' && is_dir($entry))
		{ 
			//echo "<center><font size=2><b>sw entry: $entry</b></font></center>";

			//change directory to Barcode name
			if( is_software_available($entry) )
			{
				$sw_path = "$root_path/$entry";
				chdir($sw_path);
				$sw_dir = opendir($sw_path);

				while (($crc_entry = readdir($sw_dir)) !== false) 
				{
					if($crc_entry != '.' && $crc_entry != '..' && is_dir($crc_entry))
					{ 
						//echo "<center><font size=2><b>crc entry: $crc_entry</b></font></center>";

						$all_sw[] = $entry;
						$all_crc[] = $crc_entry;
						$all_status[] = check_software_status( $entry, $crc_entry );
					}
				} // end of while for a each build CRc in a software version
				closedir($sw_dir);
				chdir("..");
			}
		}
	} // end of while loop for each software version
	closedir($root_dir);
	chdir($root_path);
}

function is_software_available($a_sw)
{
	global $board_file;

	$sholes_str = "SHOLS";
	$sholes_match = "SHOLS_U2_01.14.0";
	$sholes_match_upper = "SHOLS_U2_03.00.0";
	$sholes_tellus_match = "SHOLS_U2_03.03.0";
	$return_result = false;

	if( strstr( $a_sw, $sholes_str ) )
	{
		if( ( strcmp( $a_sw, $sholes_match) >= 0 ) && ( strcmp( $a_sw, $sholes_match_upper) < 0 ) )
		//if( ( strcmp( $a_sw, $sholes_match) >= 0 ) )
		{
			$return_result = true;
		}
		elseif( strcmp( $a_sw, $sholes_tellus_match) >= 0 )
		{
			$return_result = true;
		}
	}

	// test
	if( ($board_file == "test.php") && (strcmp( $a_sw, "SHOLS_U2_00.59.0D") == 0) )
	{
		$return_result = true;
	}

	return $return_result;
}

function check_software_status($a_sw, $a_crc)
{
	global $sw_status_file;

	$retrun_str = "Unknown";
	
	if (file_exists($sw_status_file))
	{
		$line=file($sw_status_file);
		$n_line=count($line);
	}
	else
	{
		//debug_print("The software status file doesn't exist");
	}

	for ( $i = 0 ; $i < $n_line ; $i++ )
	{
		//debug_print( "line $i: $line[$i]" );
		$current_line = split("\|", $line[$i]);
		if ( ($a_sw == $current_line[0]) && ($a_crc == $current_line[1]) )
		{
			return $current_line[2];
		}	
	}

	return $retrun_str;
}

function show_all_software()
{
	global $board_file;
	global $td_color;
	$td_body="<td bgcolor=\"$td_color\" align=center><font size=2>";
	$td_body_left="<td bgcolor=\"$td_color\" align=left><font size=2>";

	global $account, $root_path, $root_url, $sw_version, $build_crc, $sort_order;
	global $all_sw, $all_crc, $all_status;

	$n_sw=count($all_sw);

	if( $n_sw == 0 )
	{
		return;
	}

	table_head_show_all_software();

	if( $sort_order == 1 )
	{
		arsort($all_sw, SORT_REGULAR);
	}
	else
	{
		asort($all_sw, SORT_REGULAR);
	}

	foreach ($all_sw as $key => $value) {
		echo "\n<tr>\n";
		// $value is the same as $all_sw[$key], if the array is sorted by $all_sw
		echo "$td_body$all_sw[$key]</td>\n";
		echo "$td_body";
		echo ("<a href=\"$board_file?job=single&sw=$all_sw[$key]&crc=$all_crc[$key]&grp=java\">$all_crc[$key]</a></font></td>\n");
		echo "$td_body$all_status[$key]</td>\n";
		echo "</tr>\n";

	}

	echo "</table></td></tr></table><br>\n";
}

function show_all_official_software()
{
	global $board_file;
	global $td_color;
	$td_body="<td bgcolor=\"$td_color\" align=center><font size=2>";
	$td_body_left="<td bgcolor=\"$td_color\" align=left><font size=2>";

	global $account, $root_path, $root_url, $sw_version, $build_crc, $sort_order;
	global $all_sw, $all_crc, $all_status;

	$n_sw=count($all_sw);

	if( $n_sw == 0 )
	{
		return;
	}

	table_head_show_all_software();

	if( $sort_order == 1 )
	{
		arsort($all_sw, SORT_REGULAR);
	}
	else
	{
		asort($all_sw, SORT_REGULAR);
	}

	foreach ($all_sw as $key => $value) {
		if ( trim($all_status[$key]) == "Official" )
		{
		//trim($key1)
			echo "\n<tr>\n";
			// $value is the same as $all_sw[$key], if the array is sorted by $all_sw
			echo "$td_body$all_sw[$key]</td>\n";
			echo "$td_body";
			echo ("<a href=\"$board_file?job=single&sw=$all_sw[$key]&crc=$all_crc[$key]&grp=java\">$all_crc[$key]</a></font></td>\n");
			echo "$td_body$all_status[$key]</td>\n";
			echo "</tr>\n";
		}
	}

	echo "</table></td></tr></table><br>\n";
}

function show_all_test_software()
{
	global $board_file;
	global $td_color;
	$td_body="<td bgcolor=\"$td_color\" align=center><font size=2>";
	$td_body_left="<td bgcolor=\"$td_color\" align=left><font size=2>";

	global $account, $root_path, $root_url, $sw_version, $build_crc, $sort_order;
	global $all_sw, $all_crc, $all_status;

	$n_sw=count($all_sw);

	if( $n_sw == 0 )
	{
		return;
	}

	table_head_show_all_software();

	if( $sort_order == 1 )
	{
		arsort($all_sw, SORT_REGULAR);
	}
	else
	{
		asort($all_sw, SORT_REGULAR);
	}

	foreach ($all_sw as $key => $value) {
		if ( trim($all_status[$key]) != "Official" )
		{
		//trim($key1)
			echo "\n<tr>\n";
			// $value is the same as $all_sw[$key], if the array is sorted by $all_sw
			echo "$td_body$all_sw[$key]</td>\n";
			echo "$td_body";
			echo ("<a href=\"$board_file?job=single&sw=$all_sw[$key]&crc=$all_crc[$key]&grp=java\">$all_crc[$key]</a></font></td>\n");
			echo "$td_body$all_status[$key]</td>\n";
			echo "</tr>\n";
		}
	}

	echo "</table></td></tr></table><br>\n";
}

// get all packages from a software version with specified build CRC
function get_all_packages()
{
	global $account, $root_path, $root_url, $sw_version, $build_crc;
	global $panic, $panic_dir;

	$build_path = "$root_path/$sw_version/$build_crc";

	// check if the directory for the software version and the build CRC exists
	if (!file_exists($build_path)){
		echo "<center><font size=2><b>$build_path does not exist.</b></font></center>";
		return false;
	}
	else{
		//echo "<center><font size=2><b>$build_path exists.</b></font></center>";
		chdir($build_path);
		$build_dir = opendir($build_path);

		while (($entry = readdir($build_dir)) !== false) {
			if($entry != '.' && $entry != '..' && is_dir($entry)){ 
				//echo "<center><font size=2><b>barcode entry: $entry</b></font></center>";

				//change directory to Barcode name
				$bar_path = "$build_path/$entry";
				chdir($bar_path);
				$bar_dir = opendir($bar_path);

				// read name of packages in the barcode directory
				while (($package_entry = readdir($bar_dir)) !== false) {
					if($package_entry != '.' && $package_entry != '..' && is_dir($package_entry)){ 
						//echo "<center><font size=2><b>package entry: $package_entry</b></font></center>";

						$package_path = "$bar_path/$package_entry";

						// collect panic packages
						get_panic_package($package_path);
					}
				} // end of while for a each package directory in a barcode
				closedir($bar_dir);
				chdir("..");
			}
		} // end of while loop for each barcode
		closedir($build_dir);
		chdir($root_path);
	}
}

// chcek if the direcory includes panic data
function get_panic_package($package_path)
{
	global $panic, $panic_dir;

	$package_list = "$package_path/package_list.txt";
	$panic_string = "PanicData:";
	//$return_result = "false";
	
	//echo "<center><font size=2><b>package entry: $package_path</b></font></center>";
	// check if there is package_list.txt in the directory
	if (!file_exists($package_list)){
		//echo "<center><font size=2><b>$package_list does not exist.</b></font></center>";
	}
	else
	{
		//read lines in the package_list.txt
		$index=file($package_list);
		$n_index=count($index);

		for ( $i = 0 ; $i < $n_index; $i++ )
		{
			//echo "$index[$i]<br>";
			// check if the line includes the panic string
			if( strstr( $index[$i], $panic_string ) ){
				$panic[] = $index[$i];
				$panic_dir[] = $package_path;
				//$return_result = "false";
				//echo "$index[$i]<br>";
				//echo "<center><font size=2><b>$index[$i]</b></font></center>";
			}
		}
	}
	//return $return_result;
}

// classify panics according to the panic id
function classify_panic_packages()
{
	global $panic, $panic_dir;
	global $java_app_crash, 		$java_app_crash_dir;
	global $java_app_crash_anr, 	$java_app_crash_anr_dir;
	global $linux_user_space_crash, 	$linux_user_space_crash_dir;
	global $unknown_crash_type, 	$unknown_crash_type_dir;
	global $kernel_crash, 		$kernel_crash_dir;
	global $modem_crash, 		$modem_crash_dir;
	global $power_cut, 		$power_cut_dir;
	global $ap_watchdog, 		$ap_watchdog_dir;
	global $cpcap_watchdog, 		$cpcap_watchdog_dir;
	global $kernel_apanic_nd, 		$kernel_apanic_nd_dir;
	global $bp_panic, 			$bp_panic_dir;
	global $other_panic, $other_panic_id, $other_panic_dir;

	$offset_str = "(PanicID)";
	$len_offset_str = strlen($offset_str);

	$n_panic=count($panic);

	for ( $i = 0 ; $i < $n_panic; $i++ )
	{
		//echo "<center><font size=2><b>$panic[$i]</b></font></center>";
		//echo "<center><font size=2><b>$panic_dir[$i]</b></font></center>";
		//echo "<b>$panic[$i]</b><br>";
		if( $panic_id_str = strstr( $panic[$i], $offset_str) ){
			//echo "<b>$panic_id_str</b><br>";

			// get the position of space after the panic id
			$space_pos = strpos( $panic_id_str, ' ' );
			//echo "<b>position: $space_pos</b><br>";

			// get panic id from the string
			$panic_id = substr( $panic_id_str, $len_offset_str, $space_pos - $len_offset_str);
			//echo "<b>panic id: $panic_id</b><br>";

			switch( $panic_id )
			{
				case JAVA_APP_CRASH_ID: // A0080524
					//echo "<b>JAVA_APP_CRASH</b><br>";
					$java_app_crash[] = $panic_id_str;
					$java_app_crash_dir[] = $panic_dir[$i];
					break;

				case JAVA_APP_CRASH_ANR_ID: // A0080529
					//echo "<b>JAVA_APP_CRASH_ANR</b><br>";
					$java_app_crash_anr[] = $panic_id_str;
					$java_app_crash_anr_dir[] = $panic_dir[$i];
					break;

				case LINUX_USER_SPACE_CRASH_ID: // A0080523
					//echo "<b>LINUX_USER_SPACE_CRASH</b><br>";
					$linux_user_space_crash[] = $panic_id_str;
					$linux_user_space_crash_dir[] = $panic_dir[$i];
					break;
/* Block details for critical panics */
/*
				case UNKNOWN_CRASH_TYPE_ID: // A0080500
					//echo "<b>UNKNOWN_CRASH_TYPE</b><br>";
					$unknown_crash_type[] = $panic_id_str;
					$unknown_crash_type_dir[] = $panic_dir[$i];
					break;

				case KERNEL_CRASH_ID: // A0080520
					//echo "<b>KERNEL_CRASH</b><br>";
					$kernel_crash[] = $panic_id_str;
					$kernel_crash_dir[] = $panic_dir[$i];
					break;

				case MODEM_CRASH_ID: // A0080501
					//echo "<b>MODEM_CRASH</b><br>";
					$modem_crash[] = $panic_id_str;
					$modem_crash_dir[] = $panic_dir[$i];
					break;

				case POWER_CUT_ID: // A0080525
					//echo "<b>POWER_CUT</b><br>";
					$power_cut[] = $panic_id_str;
					$power_cut_dir[] = $panic_dir[$i];
					break;

				case AP_WATCHDOG_ID: // A0080526
					//echo "<b>AP_WATCHDOG</b><br>";
					$ap_watchdog[] = $panic_id_str;
					$ap_watchdog_dir[] = $panic_dir[$i];
					break;

				case CPCAP_WATCHDOG_ID: // A0080527
					//echo "<b>CPCAP_WATCHDOG</b><br>";
					$cpcap_watchdog[] = $panic_id_str;
					$cpcap_watchdog_dir[] = $panic_dir[$i];
					break;

				case KERNEL_APANIC_ND_ID: // A0080528
					//echo "<b>KERNEL_APANIC_ND</b><br>";
					$kernel_apanic_nd[] = $panic_id_str;
					$kernel_apanic_nd_dir[] = $panic_dir[$i];
					break;

				default: // BP_PANIC
					//echo "<b>BP_PANIC, ID: $panic_id</b><br>";
					$bp_panic[] = $panic_id_str;
					$bp_panic_dir[] = $panic_dir[$i];
					break;
*/
				default: // Other panics
					//echo "<b>PANIC ID: $panic_id</b><br>";
					$other_panic_id[] = $panic_id;
					$other_panic[] = $panic_id_str;
					$other_panic_dir[] = $panic_dir[$i];
					break;
			}
		}
	}

}

function get_panic_name( $panic_id )
{
	switch( $panic_id )
	{
		case JAVA_APP_CRASH_ID: // A0080524
			//echo "<b>JAVA_APP_CRASH</b><br>";
			$return_str = "JAVA_APP_CRASH";
			break;

		case JAVA_APP_CRASH_ANR_ID: // A0080529
			//echo "<b>JAVA_APP_CRASH_ANR</b><br>";
			$return_str = "AVA_APP_CRASH_ANR";
			break;

		case LINUX_USER_SPACE_CRASH_ID: // A0080523
			//echo "<b>LINUX_USER_SPACE_CRASH</b><br>";
			$return_str = "LINUX_USER_SPACE_CRASH";
			break;

		case UNKNOWN_CRASH_TYPE_ID: // A0080500
			//echo "<b>UNKNOWN_CRASH_TYPE</b><br>";
			$return_str = "Unknown";
			break;

		case KERNEL_CRASH_ID: // A0080520
			//echo "<b>KERNEL_CRASH</b><br>";
			$return_str = "Kernel Crash";
			break;

		case MODEM_CRASH_ID: // A0080501
			//echo "<b>MODEM_CRASH</b><br>";
			$return_str = "Modem Crash";
			break;

		case POWER_CUT_ID: // A0080525
			//echo "<b>POWER_CUT</b><br>";
			$return_str = "POWER CUT";
			break;

		case AP_WATCHDOG_ID: // A0080526
			//echo "<b>AP_WATCHDOG</b><br>";
			$return_str = "AP WATCHDOG";
			break;

		case CPCAP_WATCHDOG_ID: // A0080527
			//echo "<b>CPCAP_WATCHDOG</b><br>";
			$return_str = "CPCAP WATCHDOG";
			break;

		case KERNEL_APANIC_ND_ID: // A0080528
			//echo "<b>KERNEL_APANIC_ND</b><br>";
			$return_str = "Kernel Panic No Data";
			break;

		default: // BP_PANIC and other
			//echo "<b>PANIC ID: $panic_id</b><br>";
			$return_str = check_panic_name_table($panic_id);
			break;
	}
	return $return_str;
}

function check_panic_name_table($a_panic_id)
{
	global $panic_name_file;

	$retrun_str = "Undefined";
	
	if (file_exists($panic_name_file))
	{
		$line=file($panic_name_file);
		$n_line=count($line);
	}
	else
	{
		//debug_print("The panic name file doesn't exist");
	}

	for ( $i = 0 ; $i < $n_line ; $i++ )
	{
		//debug_print( "line $i: $line[$i]" );
		$current_line = split("\|", $line[$i]);
		if ( $a_panic_id == $current_line[0] )
		{
			return $current_line[1];
		}	
	}

	return $retrun_str;
}

// read barcodefrom the package name
function read_barcode( $package_dir )
{
	global $build_crc;

	//echo "<b>package dir: $package_dir</b><br>";

	$package_name = strrchr( $package_dir, '/' );
	//echo "<b>package name: $package_name</b><br>";

	//echo "<b>build crc: $build_crc</b><br>";

	$str_from_build_crc = strstr( $package_name, $build_crc);
	//echo "<b>str from build CRC: $str_from_build_crc</b><br>";

	// get length of build crc to get the position of the barcode
	$len_build_crc = strlen($build_crc);

	// string form the barcode
	$barcode_str = substr( $str_from_build_crc, $len_build_crc + 1);
	//echo "<b>str from barcode: $barcode_str</b><br>";

	// get the dash position between barcode and date_time
	$dash_pos = strpos( $barcode_str, '-' );

	// get barcode string
	$barcode = substr( $barcode_str, 0, $dash_pos);

	return $barcode;
}

// read date and time from the package name
function read_date_time( $package_dir )
{
	//debug_print( "package dir: $package_dir" );
	
	$package_name = strrchr( $package_dir, '/' );
	//debug_print( "package name: $package_name" );

	// get the last 15 bytes which mean YYYYMMDD_HHMMSS
	$date_time = substr( $package_dir, -15, 15 );

	$year = substr( $date_time, 0, 4 );
	$month = substr( $date_time, 4, 2 );
	$date = substr( $date_time, 6, 2 );
	$hour = substr( $date_time, 9, 2 );
	$minute = substr( $date_time, 11, 2 );
	$second = substr( $date_time, 13, 2 );

	$formated_time ="$year.$month.$date $hour:$minute:$second";

	return $formated_time;
}

function show_java_app_crash()
{
	global $td_color;
	$td_body="<td bgcolor=\"$td_color\" align=center><font size=2>";
	$td_body_left="<td bgcolor=\"$td_color\" align=left><font size=2>";

	global $account, $root_path, $root_url, $board_path, $ddts_url;
	global $sw_version, $build_crc, $panic_group, $sort_order, $sort_by;
	global $java_app_crash, 	$java_app_crash_dir;

	$build_path = "$root_path/$sw_version/$build_crc";
	$data_file ="$board_path/$sw_version-$build_crc-$panic_group.txt";

	$n_panic=count($java_app_crash);

	show_table_title( "JAVA App Crash" );
 
	if( $n_panic == 0 )
	{
		show_center_message( "No panic data" );
		return;
	}

	$offset_str = "(app_name)";
	$len_offset_str = strlen($offset_str);

	table_head_java( "java" );

	for ( $i = 0 ; $i < $n_panic; $i++ )
	{
		if( $app_name_str = strstr( $java_app_crash[$i], $offset_str) )
		{
			//echo "<b>app name str: $app_name_str</b><br>";
			
			// get app name from the string
			$app_name[$i] = substr( $app_name_str, $len_offset_str );
			//echo "<b>app name: $app_name[$i]</b><br>";
		}
		else
		{
			// set app name as 'unknown'
			$app_name[$i] = "unknown";
			//echo "<b>app name: $app_name[$i]</b><br>";
		}

		// read barcodefrom the package name
		$barcode[$i] = read_barcode( $java_app_crash_dir[$i] );

		// read date and time from the package name
		$date_time[$i] = read_date_time( $java_app_crash_dir[$i] );

		// change package path to url format
		$url[$i] = str_replace( $root_path, $root_url, $java_app_crash_dir[$i] );

		// get previous analysis if it exits
		$previous_analysis[$i] = get_analysis( $data_file, $app_name[$i], $url[$i] );
	}

	if( $sort_by == 0 )
	{
		$array_for_sorting = $app_name;
	}
	elseif( $sort_by == 1 )
	{
		$array_for_sorting = $barcode;
	}
	else
	{
		$array_for_sorting = $date_time;
	}
		
	if( $sort_order == 1 )
	{
		arsort($array_for_sorting, SORT_REGULAR);
	}
	else
	{
		asort($array_for_sorting, SORT_REGULAR);
	}

	foreach ($array_for_sorting as $key => $value)
	{
		echo "\n<tr>\n";
		echo "$td_body$app_name[$key]</td>\n";
		echo "$td_body$barcode[$key]</td>\n";
		echo "$td_body$date_time[$key]</td>\n";
		echo "$td_body<a href=\"$url[$key]\">link</a></td>\n";
		if ( substr( $previous_analysis[$key], 0, 3 ) == "LIB" )
		{
			echo "$td_body<a href=\"$ddts_url$previous_analysis[$key]\" target=\"_blank\">$previous_analysis[$key]</a></td>\n";
		}
		else
		{
			echo "$td_body$previous_analysis[$key]</td>\n";
		}
		echo "$td_body<a href=\"$board_file?job=form&sw=$sw_version&crc=$build_crc&grp=java&sort=$sort_order&by=$sort_by&app=$app_name[$key]&dir=$url[$key]&analysis=$previous_analysis[$key]\">edit</a></td>\n";
		echo "</tr>\n";
	}

	echo "</table></td></tr></table><br>";

	//print total number of panics
	if( $n_panic != 0 )
	{
		show_center_message( "Total: $n_panic" );
	}

}

function show_java_app_crash_anr()
{
	global $td_color;
	$td_body="<td bgcolor=\"$td_color\" align=center><font size=2>";
	$td_body_left="<td bgcolor=\"$td_color\" align=left><font size=2>";

	global $account, $root_path, $root_url, $board_path, $ddts_url;
	global $sw_version, $build_crc, $panic_group, $sort_order, $sort_by;
	global $java_app_crash_anr, 	$java_app_crash_anr_dir;

	$build_path = "$root_path/$sw_version/$build_crc";
	$data_file ="$board_path/$sw_version-$build_crc-$panic_group.txt";

	$n_panic=count($java_app_crash_anr);

	show_table_title( "JAVA App Crash ANR" );

	if( $n_panic == 0 )
	{
		show_center_message( "No panic data" );
		return;
	}

	$offset_str = "(app_name)";
	$len_offset_str = strlen($offset_str);

	table_head_java( "anr" );

	for ( $i = 0 ; $i < $n_panic; $i++ )
	{
		if( $app_name_str = strstr( $java_app_crash_anr[$i], $offset_str) )
		{
			//echo "<b>app name str: $app_name_str</b><br>";
			
			// get app name from the string
			$app_name[$i] = substr( $app_name_str, $len_offset_str );
			//echo "<b>app name: $app_name[$i]</b><br>";
		}
		else
		{
			// set app name as 'unknown'
			$app_name[$i] = "unknown";
			//echo "<b>app name: $app_name[$i]</b><br>";
		}

		// read barcodefrom the package name
		$barcode[$i] = read_barcode( $java_app_crash_anr_dir[$i] );

		// read date and time from the package name
		$date_time[$i] = read_date_time( $java_app_crash_anr_dir[$i] );

		// change package path to url format
		$url[$i] = str_replace( $root_path, $root_url, $java_app_crash_anr_dir[$i]);

		// get previous analysis if it exits
		$previous_analysis[$i] = get_analysis( $data_file, $app_name[$i], $url[$i] );
	}

	if( $sort_by == 0 )
	{
		$array_for_sorting = $app_name;
	}
	elseif( $sort_by == 1 )
	{
		$array_for_sorting = $barcode;
	}
	else
	{
		$array_for_sorting = $date_time;
	}
		
	if( $sort_order == 1 )
	{
		arsort($array_for_sorting, SORT_REGULAR);
	}
	else
	{
		asort($array_for_sorting, SORT_REGULAR);
	}

	foreach ($array_for_sorting as $key => $value)
	{
		echo "\n<tr>\n";
		echo "$td_body$app_name[$key]</td>\n";
		echo "$td_body$barcode[$key]</td>\n";
		echo "$td_body$date_time[$key]</td>\n";
		echo "$td_body<a href=\"$url[$key]\">link</a></td>\n";
		if ( substr( $previous_analysis[$key], 0, 3 ) == "LIB" )
		{
			echo "$td_body<a href=\"$ddts_url$previous_analysis[$key]\" target=\"_blank\">$previous_analysis[$key]</a></td>\n";
		}
		else
		{
			echo "$td_body$previous_analysis[$key]</td>\n";
		}
		echo "$td_body<a href=\"$board_file?job=form&sw=$sw_version&crc=$build_crc&grp=anr&sort=$sort_order&by=$sort_by&app=$app_name[$key]&dir=$url[$key]&analysis=$previous_analysis[$key]\">edit</a></td>\n";
		echo "</tr>\n";
	}

	echo "</table></td></tr></table><br>";

	//print total number of panics
	if( $n_panic != 0 )
	{
		show_center_message( "Total: $n_panic" );
	}
}

function show_linux_user_space_crash()
{
	global $td_color;
	$td_body="<td bgcolor=\"$td_color\" align=center><font size=2>";
	$td_body_left="<td bgcolor=\"$td_color\" align=left><font size=2>";

	global $account, $root_path, $root_url, $board_path, $ddts_url;
	global $sw_version, $build_crc, $panic_group, $sort_order, $sort_by;
	global $linux_user_space_crash, 	$linux_user_space_crash_dir;

	$build_path = "$root_path/$sw_version/$build_crc";
	$data_file ="$board_path/$sw_version-$build_crc-$panic_group.txt";

	$n_panic=count($linux_user_space_crash);

	show_table_title( "Linux User Space Crash" );

	if( $n_panic == 0 )
	{
		show_center_message( "No panic data" );
		return;
	}

	$offset_str = "(process_name)";
	$len_offset_str = strlen($offset_str);
	$pc_offset_str = "(pc)";
	$len_pc_offset_str = strlen($pc_offset_str);

	table_head_linux_user_space_crash();

	for ( $i = 0 ; $i < $n_panic; $i++ )
	{
		if( $process_name_str = strstr( $linux_user_space_crash[$i], $offset_str) )
		{
			// get the position of space after the process name
			$space_pos = strpos( $process_name_str, ' ' );

			// get process name from the string
			$process_name[$i] = substr( $process_name_str, $len_offset_str, $space_pos - $len_offset_str);
			//echo "<b>process name: $process_name[$i]</b><br>";

			if( $program_counter_str = strstr( $linux_user_space_crash[$i], $pc_offset_str) )
			{
				// get the position of space after the process name
				$space_pos = strpos( $program_counter_str, ' ' );

				// get program counter from the string
				$program_counter[$i] = substr( $program_counter_str, $len_pc_offset_str, $space_pos - $len_pc_offset_str);
				//echo "<b>program counter: $program_counter[$i]</b><br>";
			}
			else
			{
				// set program counter as 'unknown'
				$program_counter[$i] = "unknown";
				//echo "<b>program counter: $program_counter[$i]</b><br>";
			}
		}
		else
		{
			// set process name and program counter as 'unknown'
			$process_name[$i] = "unknown";
			//echo "<b>process name: $process_name[$i]</b><br>";
			$program_counter[$i] = "unknown";
			//echo "<b>program counter: $program_counter[$i]</b><br>";
		}

		// read barcodefrom the package name
		$barcode[$i] = read_barcode( $linux_user_space_crash_dir[$i] );

		// read date and time from the package name
		$date_time[$i] = read_date_time( $linux_user_space_crash_dir[$i] );

		// change package path to url format
		$url[$i] = str_replace( $root_path, $root_url, $linux_user_space_crash_dir[$i]);

		// get previous analysis if it exits
		$previous_analysis[$i] = get_analysis( $data_file, $process_name[$i], $url[$i] );
	}

	if( $sort_by == 0 )
	{
		$array_for_sorting = $process_name;
	}
	elseif( $sort_by == 1 )
	{
		$array_for_sorting = $barcode;
	}
	else
	{
		$array_for_sorting = $date_time;
	}
		
	if( $sort_order == 1 )
	{
		arsort($array_for_sorting, SORT_REGULAR);
	}
	else
	{
		asort($array_for_sorting, SORT_REGULAR);
	}

	// print content of table
	foreach ($array_for_sorting as $key => $value)
	{
		echo "\n<tr>\n";
		echo "$td_body$process_name[$key]</td>\n";
		echo "$td_body$program_counter[$key]</td>\n";
		echo "$td_body$barcode[$key]</td>\n";
		echo "$td_body$date_time[$key]</td>\n";
		echo "$td_body<a href=\"$url[$key]\">link</a></td>\n";
		if ( substr( $previous_analysis[$key], 0, 3 ) == "LIB" )
		{
			echo "$td_body<a href=\"$ddts_url$previous_analysis[$key]\" target=\"_blank\">$previous_analysis[$key]</a></td>\n";
		}
		else
		{
			echo "$td_body$previous_analysis[$key]</td>\n";
		}
		echo "$td_body<a href=\"$board_file?job=form&sw=$sw_version&crc=$build_crc&grp=ap&sort=$sort_order&by=$sort_by&app=$process_name[$key]&dir=$url[$key]&analysis=$previous_analysis[$key]\">edit</a></td>\n";
		echo "</tr>\n";
	}

	echo "</table></td></tr></table><br>";

	//print total number of panics
	if( $n_panic != 0 )
	{
		show_center_message( "Total: $n_panic" );
	}
}

function show_other_crash()
{
	global $td_color;
	$td_body="<td bgcolor=\"$td_color\" align=center><font size=2>";
	$td_body_left="<td bgcolor=\"$td_color\" align=left><font size=2>";

	global $account, $root_path, $root_url, $board_path, $ddts_url;
	global $sw_version, $build_crc, $panic_group, $sort_order, $sort_by;
	global $other_panic, $other_panic_id, $other_panic_dir;

	$build_path = "$root_path/$sw_version/$build_crc";
	$data_file ="$board_path/$sw_version-$build_crc-$panic_group.txt";

	$n_panic=count($other_panic);

	show_table_title( "BP Panic and Kernel Crash" );

	if( $n_panic == 0 )
	{
		show_center_message( "No panic data" );
		return;
	}

	$offset_str = "(app_name)";
	$len_offset_str = strlen($offset_str);

	table_head_other_panic();

	for ( $i = 0 ; $i < $n_panic; $i++ )
	{
		$panic_name[$i] = get_panic_name( $other_panic_id[$i] );
		
		if( $space_pos = strpos( $other_panic[$i], ' ' ) )
		{
			//echo "<b>space position: $space_pos</b><br>";
			
			// get panic data from the string after the panic ID
			$other_panic_str[$i] = substr( $other_panic[$i], $space_pos , 22 );
		}
		else
		{
			// set panic data as 'none'
			$other_panic_str[$i] = "none";
		}

		// read barcodefrom the package name
		$barcode[$i] = read_barcode( $other_panic_dir[$i] );

		// read date and time from the package name
		$date_time[$i] = read_date_time( $other_panic_dir[$i] );

		// change package path to url format
		$url[$i] = str_replace( $root_path, $root_url, $other_panic_dir[$i]);

		// get previous analysis if it exits
		$previous_analysis[$i] = get_analysis( $data_file, $other_panic_id[$i], $url[$i] );
	}

	if( $sort_by == 0 )
	{
		$array_for_sorting = $other_panic_id;
	}
	elseif( $sort_by == 1 )
	{
		$array_for_sorting = $barcode;
	}
	elseif( $sort_by == 2 )
	{
		$array_for_sorting = $date_time;
	}
	else
	{
		$array_for_sorting = $panic_name;
	}
		
	if( $sort_order == 1 )
	{
		arsort($array_for_sorting, SORT_REGULAR);
	}
	else
	{
		asort($array_for_sorting, SORT_REGULAR);
	}

	// print content of table
	foreach ($array_for_sorting as $key => $value)
	{
		echo "\n<tr>\n";
		echo "$td_body$other_panic_id[$key]</td>\n";
		echo "$td_body$panic_name[$key]</td>\n";
		echo "$td_body$other_panic_str[$key]</td>\n";
		echo "$td_body$barcode[$key]</td>\n";
		echo "$td_body$date_time[$key]</td>\n";
		echo "$td_body<a href=\"$url[$key]\">link</a></td>\n";
		if ( substr( $previous_analysis[$key], 0, 3 ) == "LIB" )
		{
			echo "$td_body<a href=\"$ddts_url$previous_analysis[$key]\" target=\"_blank\">$previous_analysis[$key]</a></td>\n";
		}
		else
		{
			echo "$td_body$previous_analysis[$key]</td>\n";
		}
		echo "$td_body<a href=\"$board_file?job=form&sw=$sw_version&crc=$build_crc&grp=bp&sort=$sort_order&by=$sort_by&app=$other_panic_id[$key]&dir=$url[$key]&analysis=$previous_analysis[$key]\">edit</a></td>\n";
		echo "</tr>\n";
	}

	echo "</table></td></tr></table><br>";

	//print total number of panics
	if( $n_panic != 0 )
	{
		show_center_message( "Total: $n_panic" );
	}
}

function select_panic_group()
{
	global $board_file;
	global $sw_version, $build_crc, $panic_group;

	echo("\n<center><table border=0 cellspacing=0 cellpadding=0><tr>");

	if( $panic_group == "java" )
	{
		echo ("<td><font size=2>Java App Crash</font></td>\n");
	}
	else
	{
		echo ("<td><font size=2><a href=\"$board_file?job=single&sw=$sw_version&crc=$build_crc&grp=java\">Java App Crash</a></font></td>\n");
	}

	echo ("<td><font size=2> |</font></td>\n");

	if( $panic_group == "anr" )
	{
		echo ("<td><font size=2>Java App Crash ANR</font></td>\n");
	}
	else
	{
		echo ("<td><font size=2><a href=\"$board_file?job=single&sw=$sw_version&crc=$build_crc&grp=anr\">Java App Crash ANR</a></font></td>\n");
	}

	echo ("<td><font size=2> |</font></td>\n");

	if( $panic_group == "ap" )
	{
		echo ("<td><font size=2>Linux User Space Crash</font></td>\n");
	}
	else
	{
		echo ("<td><font size=2><a href=\"$board_file?job=single&sw=$sw_version&crc=$build_crc&grp=ap\">Linux User Space Crash</a></font></td>\n");
	}

	echo ("<td><font size=2> |</font></td>\n");

	if( $panic_group == "bp" )
	{
		echo ("<td><font size=2>BP Panic and Kernel Crash</font></td>\n");
	}
	else
	{
		echo ("<td><font size=2><a href=\"$board_file?job=single&sw=$sw_version&crc=$build_crc&grp=bp\">BP Panic and Kernel Crash</a></font></td>\n");
	}

	echo("</tr></table></center>\n");

}	

// Show single panic group on a web page for the software selected
function show_panic_group()
{
	global $sw_version, $build_crc, $panic_group;

	select_panic_group();

	switch( $panic_group )
	{
		case "java": // Java App Crash
			show_java_app_crash();
			break;

		case "anr": // Java App Crash ANR
			show_java_app_crash_anr();
			break;

		case "ap": // Linux User Space Crash
			show_linux_user_space_crash();
			break;

		case "bp": // BP Panic and Kernel Crash
			show_other_crash();
			break;

		default:
			show_java_app_crash();
			break;
	}

}	

// Show all panic groups on a web page for the software selected
function show_panic_packages()
{
	show_java_app_crash();
	show_java_app_crash_anr();
	show_linux_user_space_crash();
	show_other_crash();
}	

// Show all panics for the software selected
function ShowSingleSoftware()
{
	global $sw_version, $build_crc;
	
	get_all_packages();
	classify_panic_packages();

	show_table_title( "$sw_version (Build CRC: $build_crc)" );

	show_panic_group();		

	//show_panic_packages();		
}  // End of ShowSingleSoftware()

// Show all available software version that the data matchs to APR 3.7.4 
function ShowAllSoftware()
{
	get_all_software();

	//show_table_title( "Click a Build CRC to see panic data" );
	//show_all_software();		

	show_table_title( "Official Release (Click a Build CRC to see panic data)" );
	show_all_official_software();		

	show_table_title( "Unofficial Release (Click a Build CRC to see panic data)" );
	show_all_test_software();		
}

function ShowAanlysisForm()
{
	global $board_file;
	global $sw_version, $build_crc, $panic_group, $sort_order, $sort_by;
	global $current_app, $current_dir, $current_analysis;

echo("
<p><p><p>
<center><table>
<tr><td align=center><font size=3><b>Edit Analysis</b></font></td></tr>
</table></center><p>
<center>
<form action=$board_file?job=write method=\"post\">
<input type=\"hidden\" name=\"sw\" value=$sw_version>
<input type=\"hidden\" name=\"crc\" value=$build_crc>
<input type=\"hidden\" name=\"grp\" value=$panic_group>
<input type=\"hidden\" name=\"sort\" value=$sort_order>
<input type=\"hidden\" name=\"by\" value=$sort_by>
<input type=\"hidden\" name=\"app\" value=$current_app>
<input type=\"hidden\" name=\"dir\" value=$current_dir>
<table width=600 border=0 cellspacing=0>
<tr><td align=center valign=middle><font size=3>Enter the CR number submitted or add analysis</td>
<tr><td align=center valign=middle>
<input name=\"analysis\" type=\"text\" size=80 maxlength=80 value=\"$current_analysis\"></td></tr>
<tr><td colspan=2 valign=bottom align=center>
<input type=\"submit\" value=\"Submit\">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type=\"reset\" value=\"Reset\"></td></tr>
</form>
</center>
");

}

function WriteAanlysis()
{
	global $root_path, $board_path;
	global $sw_version, $build_crc, $panic_group, $sort_order, $sort_by;
	global $current_app, $current_dir, $current_analysis;

	$found_previous_analysis = false;

	$build_path = "$root_path/$sw_version/$build_crc";
	$data_file ="$board_path/$sw_version-$build_crc-$panic_group.txt";

/*
	if( $sort_by == 0 )
	{
		echo ("by: 0 <br>");
	}
	elseif( $sort_by == 1 )
	{
		echo ("by: 1 <br>");
	}
	elseif( $sort_by == 2 )
	{
		echo ("by: 2<br>");
	}

		
	if( $sort_order == 1 )
	{
		echo ("order: 1<br>");
	}
	else
	{
		echo ("order: 0<br>");
	}
*/

	if (file_exists($data_file))
	{
		$line=file($data_file);
		$n_line=count($line);
	}
	else
	{
		//debug_print("The analysis file doesn't exist");
		$n_line = 0;
	}

	for ( $i = 0 ; $i < $n_line ; $i++ )
	{
		//debug_print( "line $i: $line[$i]" );
		$current_line = split("\|", $line[$i]);
		if ( ($current_app == $current_line[0]) && ($current_dir == $current_line[1]) )
		{
			$current_line[2] = $current_analysis;
			$line[$i] = "$current_app|$current_dir|$current_analysis\n";
			$found_previous_analysis = true;
			break;
		}	
	}

	if( $found_previous_analysis == false )
	{
		$line[$n_line] = "$current_app|$current_dir|$current_analysis\n";
		$n_line++;
	}

	$fp = fopen($data_file, "w");
	for ($i=0; $i<=$n_line ; $i++)
	{
		fwrite($fp, $line[$i]);
	}
	fclose($fp);
/*
	echo ("sw: $sw_version <br>");
	echo ("crc: $build_crc <br>");
	echo ("panic group: $panic_group <br>");
	echo ("app: $current_app <br>");
	echo ("dir: $current_dir <br>");
	echo ("analysis: $current_analysis <br>");
*/
}

// read the analysis file and compare key1 and string1, key2 and string2.
// return 3rd string if strings are matched.
function get_analysis( $data_file, $key1, $key2 )
{
	$current_line = array();

	if (file_exists($data_file))
	{
		$line=file($data_file);
		$n_line=count($line);
	}
	else
	{
		//debug_print("The analysis file doesn't exist");
	}

	for ( $i = 0 ; $i < $n_line ; $i++ )
	{
		//debug_print( "line $i: $line[$i]" );
		$current_line = split("\|", $line[$i]);
		//debug_print( "key0: $key1" );
		//debug_print( "key1: $key2" );
		//debug_print( "current 0: $current_line[0]" );
		//debug_print( "current 1: $current_line[1]" );
		//debug_print( "current 2: $current_line[2]" );
		if( ( trim($key1) == trim($current_line[0]) ) && ( trim($key2) == trim($current_line[1]) ) )
		{
			//debug_print( "current result: $current_line[2]" );
			return $current_line[2];
		}	
	}
}

function show_root_link()
{
	global $board_file;

	echo("<center><a href=\"$board_file?job=root\">Lists of the root directory</a></center>");
	echo("<br>");
}

/////////////////////////////////////////
// Program start

// Print head file
ReadFilePrint($head_file);

show_main_title("Milestone APR FTP");

if ($_GET['job']=="single"){
	$sw_version = $_GET['sw'];
	$build_crc = $_GET['crc'];
	$panic_group = $_GET['grp'];
	$sort_order = $_GET['sort'];
	$sort_by = $_GET['by'];

	ShowSingleSoftware();
}
elseif ($_GET['job']=="all"){
	//debug_print("job=all selected");
	$sort_order = $_GET['sort'];

	ShowAllSoftware();
	show_root_link();
}
elseif ($_GET['job']=="form"){
	//debug_print("job=form selected");
	$sw_version = $_GET['sw'];
	$build_crc = $_GET['crc'];
	$panic_group = $_GET['grp'];
	$sort_order = $_GET['sort'];
	$sort_by = $_GET['by'];
	$current_app = $_GET['app'];
	$current_dir = $_GET['dir'];
	$current_analysis = $_GET['analysis'];

	ShowAanlysisForm();
}
elseif ($_GET['job']=="write"){
	//debug_print("job=write selected");
	$sw_version = $_POST['sw'];
	$build_crc = $_POST['crc'];
	$panic_group = $_POST['grp'];
	$sort_order = $_POST['sort'];
	$sort_by = $_POST['by'];
	$current_app = $_POST['app'];
	$current_dir = $_POST['dir'];
	$current_analysis = $_POST['analysis'];

	WriteAanlysis();
	ShowSingleSoftware();
}
elseif ($_GET['job']=="root"){
	ShowRootDirectory();
}
else {
	ShowAllSoftware();
	show_root_link();
}

// Print tail file
ReadFilePrint($tail_file);

?>
