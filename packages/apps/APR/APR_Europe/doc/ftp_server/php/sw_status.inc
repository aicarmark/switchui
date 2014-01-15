<?php

//
// Sholes UMTS panic website version 1.0
// 2009. 12. 11. 
// Paul Lee
//

// name of php
$board_file="sw_status.php";

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

$td_color = "#E9EDFF";
$th_color = "#B0B9E2";
$table_color = "#1E2A63";

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

function ShowSWStatusForm()
{
	global $board_file;
	global $sw_version, $build_crc, $sw_status;


echo("
<p><p><p>
<center><table>
<tr><td align=center><font size=3><b>Change Software Status</b></font></td></tr>
</table></center><p>
<center>
<form action=$board_file?job=write method=\"post\">
<table width=450 border=0 cellspacing=0>
<tr><td align=center valign=middle width=100><font size=3> SW version </td>
<td align=left width=350><input name=\"sw\" type=\"text\" size=30 maxlength=50></td></tr>
<tr><td align=center valign=middle width=100><font size=3> Build CRC </td>
<td align=left width=350><input name=\"crc\" type=\"text\" size=30 maxlength=50></td></tr>
<tr><td align=center valign=middle width=100><font size=3> Status </td>
<td align=left width=350><input name=\"status\" type=\"text\" size=30 maxlength=50></td></tr>
<tr><td colspan=2 valign=bottom align=center>
<input type=\"submit\" value=\"Submit\">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type=\"reset\" value=\"Reset\"></td></tr>
</form>
</center>
");

}

function WriteSWStatus()
{
	global $root_path, $board_path;
	global $sw_version, $build_crc, $sw_status;
	global $sw_status_file;

	$old_status_changed = false;
	
	if (file_exists($sw_status_file))
	{
		$line=file($sw_status_file);
		$n_line=count($line);
	}
	else
	{
		debug_print("The software status file doesn't exist");
		$n_line = 0;
	}

	for ( $i = 0 ; $i < $n_line ; $i++ )
	{
		//debug_print( "line $i: $line[$i]" );
		$current_line = split("\|", $line[$i]);
		if ( ($sw_version == $current_line[0]) && ($build_crc == $current_line[1]) )
		{
			$line[$i] = "$current_line[0]|$current_line[1]|$sw_status\n";
			$old_status_changed = true;
			break;
		}	
	}

	if( $old_status_changed == false )
	{
		$line[$n_line] = "$sw_version|$build_crc|$sw_status\n";
		$n_line++;
	}

	$fp = fopen($sw_status_file, "w");
	for ($i=0; $i<=$n_line ; $i++)
	{
		fwrite($fp, $line[$i]);
	}
	fclose($fp);	
}

function show_root_link()
{
	global $board_file;

	echo("<center><a href=\"$board_file?job=root\">Lists of the root directory</a></center>");
}

/////////////////////////////////////////
// Program start

// Print head file
ReadFilePrint($head_file);

show_main_title("Sholes UMTS Panic web");

if ($_GET['job']=="form"){
	//debug_print("job=form selected");
	ShowSWStatusForm();
}
elseif ($_GET['job']=="write"){
	//debug_print("job=write selected");
	$sw_version = $_POST['sw'];
	$build_crc = $_POST['crc'];
	$sw_status= $_POST['status'];
	//debug_print( "sw_version: $sw_version" );
	//debug_print( "build_crc: $build_crc" );
	//debug_print( "sw_status: $sw_status" );

	WriteSWStatus();
	ShowSWStatusForm();
}
else {
	ShowSWStatusForm();
}

// Print tail file
ReadFilePrint($tail_file);

?>
