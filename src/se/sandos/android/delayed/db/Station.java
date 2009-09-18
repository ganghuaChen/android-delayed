package se.sandos.android.delayed.db;

import android.os.Parcel;
import android.os.Parcelable;

public class Station implements Parcelable {
	private String mName;
	private String mUrl;
	
	public Station(String name, String url)
	{
		mName = name;
		mUrl = url;
	}	
	
	public String getName() {
		return mName;
	}

	public String getUrl() {
		return mUrl;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mName);
		dest.writeString(mUrl);
	}
	
	public static Parcelable.Creator<Station> CREATOR = new Parcelable.Creator<Station>(){

		public Station createFromParcel(Parcel source) {
			return new Station(source.readString(), source.readString());
		}

		public Station[] newArray(int size) {
			return new Station[size];
		}
	};
}
