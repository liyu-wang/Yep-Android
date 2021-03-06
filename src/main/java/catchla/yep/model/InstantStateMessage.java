package catchla.yep.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.hannesdorfmann.parcelableplease.annotation.ParcelablePlease;
import com.hannesdorfmann.parcelableplease.annotation.ParcelableThisPlease;

/**
 * Created by mariotaku on 15/11/13.
 */
@ParcelablePlease
@JsonObject
public class InstantStateMessage implements Parcelable {
    public static final Creator<InstantStateMessage> CREATOR = new Creator<InstantStateMessage>() {
        @Override
        public InstantStateMessage createFromParcel(Parcel in) {
            return new InstantStateMessage(in);
        }

        @Override
        public InstantStateMessage[] newArray(int size) {
            return new InstantStateMessage[size];
        }
    };
    @ParcelableThisPlease
    @JsonField(name = "state")
    String state;
    @ParcelableThisPlease
    @JsonField(name = "user")
    User user;
    @ParcelableThisPlease
    @JsonField(name = "recipient_type")
    String recipientType;
    @ParcelableThisPlease
    @JsonField(name = "recipient_id")
    String recipientId;

    public InstantStateMessage() {

    }

    public InstantStateMessage(Parcel src) {
        InstantStateMessageParcelablePlease.readFromParcel(this, src);
    }

    public static InstantStateMessage create(final String state) {
        final InstantStateMessage message = new InstantStateMessage();
        message.setState(state);
        return message;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public User getUser() {
        return user;
    }

    public void setUser(final User user) {
        this.user = user;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(final String recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(final String recipientType) {
        this.recipientType = recipientType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        InstantStateMessageParcelablePlease.writeToParcel(this, dest, flags);
    }
}
