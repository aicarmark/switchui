Directions for the build_tools, and integration into a Motorola/Blur-Like Make
System.

Typically, the <root>/build/tools directory contains tools that are built and
made available to the system to execute.  the gen_apr_crc directory, with
Android.mk- renamed to Android.mk, should be included in your build/tools
directory, and compiled prior to executing the main CRC upload script
(upload_apr_crc).

upload_apr_crc, included in this directory, is used in olympus in the
<root>/mot/bin directory.  after grabbing data from the .sbf file, it will
calculate the CRC using gen_apr_crc, and upload it to the LV APR server using
Map_Upload.pl

USAGE:

# Do a real upload
upload_apr_crc -s <sbf file or sbf.gz file>

# Do a test: does everything but the upload
upload_apr_crc -t -s <sbf file or sbf.gz file>

Right now, upload_apr_crc expects the executable gen_apr_crc to be located,
after compile time, here

${REPO_ROOT}/out/host/linux-x86/bin/gen_apr_crc

