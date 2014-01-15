#/bin/sh


fpick=/home/e13010/repo_storage/IronPrimJB/motorola/pick

rm -rf $fpick

mkdir $fpick

echo "fpick=$fpick"


cp motorola/packages/apps/FireWall/src/com/motorola/firewall/CallFirewallCallback.aidl $fpick
cp motorola/packages/apps/FireWall/src/com/motorola/firewall/SmsFirewallCallback.aidl $fpick
cp motorola/packages/apps/FireWall/src/com/android/phone/PhoneHub.aidl $fpick
cp motorola/packages/apps/FireWall/src/com/android/mms/SmsHub.aidl $fpick
#cp out/target/product/generic/system/framework/com.motorola.calendarcommon.jar $fpick
#cp packages/apps/Calendar/library/com.motorola.calendarcommon.xml  $fpick
cp motorola/packages/apps/BatteryManager/src/com/motorola/batterymanager/IBatteryManager.aidl $fpick
cp motorola/packages/apps/BatteryManager/src/com/motorola/batterymanager/IWlMonitor.aidl $fpick
cp out/target/product/generic/system/lib/libqnutil.so $fpick
cp motorola/packages/apps/NumberLocation/app/src/com/motorola/numberlocation/INumberLocationCallback.aidl $fpick
cp motorola/packages/apps/NumberLocation/app/src/com/motorola/numberlocation/INumberLocationService.aidl $fpick
cp out/target/product/generic/system/app/Firewall.apk  $fpick
cp out/target/product/generic/system/app/ActivityGraphEx.apk $fpick
cp out/target/product/generic/system/app/Calendar.apk  $fpick
cp out/target/product/generic/system/app/CalendarProvider.apk  $fpick
cp out/target/product/generic/system/app/BatteryManager.apk  $fpick
cp out/target/product/generic/system/app/QuickNote.apk  $fpick
cp out/target/product/generic/system/app/ChinaNumberLocation.apk  $fpick
cp out/target/product/generic/system/app/Contacts.apk  $fpick
cp out/target/product/generic/system/app/ContactsProvider.apk  $fpick
cp out/target/product/generic/system/app/SocialGraphEx.apk  $fpick
cp out/target/product/generic/system/app/MotoHomeEx.apk  $fpick
cp out/target/product/generic/system/app/WeatherEx.apk  $fpick
cp out/target/product/generic/system/app/RssWidgetEx.apk  $fpick
cp out/target/product/generic/system/app/PerformanceMaster.apk  $fpick
cp out/target/product/generic/system/app/ToggleWidgets_PRC.apk  $fpick
cp out/target/product/generic/system/app/APR_Flex.apk  $fpick
cp out/target/product/generic/system/app/TaskManager.apk  $fpick
cp out/target/product/generic/system/app/SDBackup.apk  $fpick
cp out/target/product/generic/system/app/TaskManager.apk  $fpick
cp out/target/product/generic/system/app/FileManager.apk  $fpick
cp out/target/product/generic/system/app/WorldClock.apk  $fpick
cp out/target/product/generic/system/app/SmartActionMMCP.apk  $fpick
cp out/target/product/generic/system/app/MMCPAcousticWarning.apk  $fpick
cp motorola/packages/apps/SmartAction/PRC/SmartActionFW.apk  $fpick
cp motorola/packages/apps/SmartAction/PRC/SmartActions.apk  $fpick
cp motorola/packages/apps/SmartAction/PRC/com.motorola.smartaction.xml  $fpick
cp motorola/packages/apps/SmartAction/PRC/com.motorola.smartactions_blacklist.xml  $fpick
cp motorola/packages/apps/SmartAction/PRC/com.motorola.smartactions_ruleblacklist.xml  $fpick
cp motorola/packages/apps/SmartAction/PRC/com.motorola.smartactions_whitelist.xml  $fpick




cd $fpick
tar -cvf Firewall.apk.tar  Firewall.apk CallFirewallCallback.aidl SmsFirewallCallback.aidl PhoneHub.aidl SmsHub.aidl
#tar -cvf MotoCalendar.apk.tar  MotoCalendar.apk  CalendarProvider.apk  com.motorola.calendarcommon.jar  com.motorola.calendarcommon.xml
tar -cvf BatteryManager.apk.tar  BatteryManager.apk  IBatteryManager.aidl  IWlMonitor.aidl 
tar -cvf QuickNote.apk.tar  QuickNote.apk  libqnutil.so
tar -cvf ChinaNumberLocation.apk.tar  ChinaNumberLocation.apk  INumberLocationCallback.aidl  INumberLocationService.aidl
#tar -cvf Contacts.apk.tar  Contacts.apk  ContactsProvider.apk
tar -cvf SmartActionMMCP.apk.tar SmartActionMMCP.apk SmartActionFW.apk SmartActions.apk com.motorola.smartaction.xml com.motorola.smartactions_blacklist.xml com.motorola.smartactions_ruleblacklist.xml com.motorola.smartactions_whitelist.xml    
cd -




curdir=$PWD

echo "$curdir"

cd $curdir

for list in `cat motorola/build/ironprimjbmatrix.list`


	do
		if [ -f DirInfo.txt ]

		then

			rm DirInfo.txt

		fi

		echo $list > DirInfo.txt
		
		gitname=`cut -d: -f 1 DirInfo.txt`

		foldername=`cut -d: -f 2 DirInfo.txt`

		apkname=`cut -d: -f 3 DirInfo.txt`

		remote=`cut -d: -f 4 DirInfo.txt`

		DIR="$curdir/$gitname" 

             	cd $DIR

      		echo "currentdir is $DIR"

		RI=I

#		tag=`git tag -l IRONPRIMJB* |tail -1`

		tag=`git tag |grep "IRONPRIMJB" |tail -1`

		if [ -f releasenote.txt ]		

		then

			rm releasenote.txt

		fi

		git log $tag..HEAD --oneline > releasenote.txt

		mversion=IRONPRIMJB_00_00_
		
		sversion=10#`echo $tag |cut -c 18-21`

		let "nsversion=(++sversion)"

		nsversion=$(printf "%04d" $nsversion )

		nversion=$mversion$nsversion$RI
		
		echo "new software verison is $nversion"


		if [ -s releasenote.txt ]

			then 


				echo "create new folder for $foldername"

				pick=$fpick/$foldername

				mkdir $pick

				psversion=$pick/$nversion

				mkdir $psversion

				echo "pick is at $pick"

				cp releasenote.txt $psversion

				cp $fpick/$apkname $psversion

git tag $nversion -m "$nversion"
git push $remote $nversion


		else


			echo "No change in $DIR"


		fi

	done


