package se.sandos.android.delayed.db;

import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class StationList implements Parcelable {

	private List<Station> data;
	
	public StationList(List<Station> data)
	{
		this.data = data;
	}
	
	public StationList(Parcel p)
	{
		p.readTypedList(data, Station.CREATOR);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		Log.i("StationListActivity", "doing2");
		dest.writeTypedList(data);
	}
	
	public static Parcelable.Creator CREATOR = new Parcelable.Creator(){

		public Object createFromParcel(Parcel source) {
			return new StationList(source);
		}

		public Object[] newArray(int size) {
			return new StationList[size];
		}
		
	};

}
