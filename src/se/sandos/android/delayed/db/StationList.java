package se.sandos.android.delayed.db;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class StationList implements Parcelable {

	private List<Station> data;
	
	public StationList(List<Station> data)
	{
		this.data = data;
	}
	
	public StationList(Parcel p)
	{
		data = new ArrayList<Station>();
		p.readTypedList(data, Station.CREATOR);
	}

	public List<Station> getList()
	{
		return data;
	}
	
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeTypedList(data);
	}
	
	public static Parcelable.Creator<StationList> CREATOR = new Parcelable.Creator<StationList>(){

		public StationList createFromParcel(Parcel source) {
			return new StationList(source);
		}

		public StationList[] newArray(int size) {
			return new StationList[size];
		}
	};
}
