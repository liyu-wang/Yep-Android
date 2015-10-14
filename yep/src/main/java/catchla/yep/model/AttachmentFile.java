package catchla.yep.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.hannesdorfmann.parcelableplease.annotation.ParcelablePlease;
import com.hannesdorfmann.parcelableplease.annotation.ParcelableThisPlease;

/**
 * Created by mariotaku on 15/10/14.
 */
@ParcelablePlease
@JsonObject
public class AttachmentFile implements Parcelable {
    public static final Creator<AttachmentFile> CREATOR = new Creator<AttachmentFile>() {
        @Override
        public AttachmentFile createFromParcel(Parcel in) {
            return new AttachmentFile(in);
        }

        @Override
        public AttachmentFile[] newArray(int size) {
            return new AttachmentFile[size];
        }
    };
    @ParcelableThisPlease
    @JsonField(name = "storage")
    String storage;
    @ParcelableThisPlease
    @JsonField(name = "expires_in")
    String expiresIn;
    @ParcelableThisPlease
    @JsonField(name = "url")
    String url;

    public AttachmentFile() {

    }

    protected AttachmentFile(Parcel in) {
        AttachmentFileParcelablePlease.readFromParcel(this, in);
    }

    public String getStorage() {
        return storage;
    }

    public String getExpiresIn() {
        return expiresIn;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        AttachmentFileParcelablePlease.writeToParcel(this, dest, flags);
    }

    @Override
    public String toString() {
        return "File{" +
                "storage='" + storage + '\'' +
                ", expiresIn='" + expiresIn + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
