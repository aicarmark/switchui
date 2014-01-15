package android_const.provider;

public class SettingsConst {

    public class System {

       /**
        * Whether the phone vibrates when it is ringing due to an incoming call. This will
        * be used by Phone and Setting apps; it shouldn't affect other apps.
        * The value is boolean (1 or 0).
        *
        * Note: this is not same as "vibrate on ring", which had been available until ICS.
        * It was about AudioManager's setting and thus affected all the applications which
        * relied on the setting, while this is purely about the vibration setting for
        * incoming calls.
        *
        */
        public static final String VIBRATE_WHEN_RINGING = "vibrate_when_ringing";

        public static final int    VIBRATE_ON = 1;

        public static final int    VIBRATE_OFF = 0;

	    // To be supplied as default value to getInt
        public static final int    DEFAULT_VALUE = -1;

        public static final String VOLUME_RING_SPEAKER = "volume_ring_speaker";
        public static final String VOLUME_ALARM_SPEAKER = "volume_alarm_speaker";
        public static final String VOLUME_MUSIC_SPEAKER = "volume_music_speaker";
        public static final String VOLUME_MUSIC_HEADSET = "volume_music_headset";
    }
}
