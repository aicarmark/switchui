
#!/bin/bash


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

		DIR="$curdir/$gitname" 

              cd $DIR

     		echo "currentdir is $DIR"

		RI=I

#		tag=`git tag -l IRONPRIMJB* |tail -1`

		tag=`git tag |grep "IRONPRIMJB" |tail -1`

		mversion=IRONPRIMJB_00_00_
		
		sversion=10#`echo $tag |cut -c 18-21`

		let "nsversion=(++sversion)"

		nsversion=$(printf "%04d" $nsversion )

		nversion=$mversion$nsversion$RI
		
		echo $nversion
		
		RESULT=`find $DIR -name "AndroidManifest.xml"|xargs grep android:versionName -l`
		
		for xml in $RESULT

		do
			`sed  -i  's/android:versionName="[^"]*"/android:versionName="'$nversion'"/' "$xml"`
			`sed  -i  's/android:versionCode="[^"]*"/android:versionCode="01"/' "$xml"`
		done
	done

		

