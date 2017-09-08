package project.sherpa.utilities;

/**
 * Created by Alvin on 8/23/2017.
 */

public class Constants {

    private Constants() {}

    public static class RequestCodes {

        private RequestCodes() {}

        public static final int REQUEST_CODE_PROFILE_PIC    = 5987;
        public static final int REQUEST_CODE_BACKDROP       = 1894;
        public static final int REQUEST_CODE_PUBLISH        = 6133;
    }

    public static class FragmentTags {

        private FragmentTags() {}

        public static final String FRAG_TAG_HOME            = "home_frag";
        public static final String FRAG_TAG_SEARCH          = "search_frag";
        public static final String FRAG_TAG_ACCOUNT         = "account_frag";
        public static final String FRAG_TAG_FAVORITE        = "favorite_frag";
        public static final String FRAG_TAG_SAVED_GUIDES    = "saved_guides_frag";
    }

    public static class IntentKeys {

        private IntentKeys() {}

        public static final String AREA_KEY     = "area";
        public static final String AUTHOR_KEY   = "author";
        public static final String GUIDE_KEY    = "guide";
        public static final String TRAIL_KEY    = "trail";
        public static final String SECTION_KEY  = "section";
    }
}
