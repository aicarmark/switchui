In the olympus product, the aprmail git repo is included in
motorola/packages/apps/

This entire git repo may reside in that location, and will be built with the
include manifest and Android.mk files.

HOWEVER, please note that there are additional instructions located in this
directory, under the build_tools directory.  These are instructions for
generating the CRC information and uploading it to the APR website.  

Please note that this is UNIQUE TO THIS IMPLEMENTATION.  You MUST include these
changes, or your CRC upload process will NOT work.  Previous CRC upload
implementations will not work with this version of APR.

