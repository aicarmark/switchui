/* Stephen Dickey 05/29/2009 */
/* this is just a quick tool to take two strings and
 * generate a LV APR Accepted CRC value, in the same
 * method that the application on the phone will generate
 * that value.  This value is then used to indcate to the
 * LV APR server, that an official version has been uploaded.
 */

#include <stdlib.h>
#include <stdio.h>

typedef unsigned short UINT16;

UINT16 APRCrc16(char *ptr, UINT16 len);


/*
 * Expeted Input
 * 
 * gen_apr_crc <username string> <date/time string>
 * e.g. 
 * gen_apr_crc wlsd10 Thu May 28 18:49:37 PDT 2009
 */

#define DEBUG 0

int main ( int argc, char *argv [] )
{
  char *crc_gen_string = NULL;

  UINT16 crc = 0;
  UINT16 total_input_length = 0;
  UINT16 i;

  if ( argc > 2 ) 
    {

    for ( i = 1; i < argc; i ++ )
      {
        /* there's a space between every input parameter */
        total_input_length += strlen( argv[i] ) + 1;
      }

    crc_gen_string = ( char *)malloc ( total_input_length );

    strcpy( crc_gen_string, argv[1] );

    for ( i = 2; i < argc; i ++ )
      {
        /* prepend a space */
        crc_gen_string = strcat( crc_gen_string, " " );
        crc_gen_string = strcat( crc_gen_string, argv[i] );
      }

#if (DEBUG==1)
    printf( "%s\n", crc_gen_string );
#endif

    crc = APRCrc16( crc_gen_string, strlen( crc_gen_string ));

    /* this is the only output
     * the input must be perfect.
     */
    printf( "%4.4X\n", crc );
    
  }
  else
  {
     printf( "Failed. Need Username and Date Time Strings For Input.\n" );
  }
}


static const UINT16 fax_util_gentab[] =
{
    0x0000, 0x1081, 0x2102, 0x3183,
    0x4204, 0x5285, 0x6306, 0x7387,
    0x8408, 0x9489, 0xa50a, 0xb58b,
    0xc60c, 0xd68d, 0xe70e, 0xf78f
};

UINT16 APRCrc16(char *ptr, UINT16 len)
{   
    UINT16 crc,c,temp;

    crc = 0xffff;   /* preset all ones      */

    while(len)
    {
        c = (UINT16)*ptr++;
        temp = ((crc & 0xff) ^ c) & 0x0f;
        temp = fax_util_gentab[temp];
        crc = (crc >> 4) ^ temp;
        c >>= 4;
        temp = ((crc & 0xff) ^ c) & 0x0f;
        temp = fax_util_gentab[temp];
        crc = (crc >> 4) ^ temp;
        crc &= 0xffff;
        len--;
    }

    return((~crc) & 0xffff);
}

