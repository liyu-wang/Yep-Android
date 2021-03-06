package catchla.yep.model;

import org.mariotaku.restfu.http.SimpleValueMap;

/**
 * Created by mariotaku on 15/5/23.
 */
public class ProfileUpdate extends SimpleValueMap {

    public void setNickname(String nickname) {
        put("nickname", nickname);
    }

    public void setUsername(String username) {
        put("username", username);
    }

    public void setBadge(String badge) {
        put("badge", badge);
    }

    public void setIntroduction(final String introduction) {
        put("introduction", introduction);
    }

    public void setWebsite(final String website) {
        put("website_url", website);
    }
}
