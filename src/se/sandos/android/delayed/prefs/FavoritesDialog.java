package se.sandos.android.delayed.prefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import se.sandos.android.delayed.R;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class FavoritesDialog extends Dialog {
    private OnClickListener listener;
    private String favoriteName;

    public FavoritesDialog(Context context) {
        super(context);

        setContentView(R.layout.favoriteslist);

        setTitle("Välj favorit att lägga till");
        setList();
    }

    public String getSelected() {
        return favoriteName;
    }

    public void setClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    public void setList() {
        List<HashMap<String, Object>> listContent = new ArrayList<HashMap<String, Object>>(10);

        List<Favorite> favs = Prefs.getFavorites(getContext());

        for (Favorite f : favs) {
            HashMap<String, Object> m = new HashMap<String, Object>();
            m.put("name", f.getName());
            listContent.add(m);
        }

        final SimpleAdapter sa = new SimpleAdapter(getContext(), listContent, R.layout.schedulerrow,
                new String[] { "name" }, new int[] { R.id.SchedulerName });

        sa.notifyDataSetInvalidated();
        final ListView lv = (ListView) findViewById(R.id.FavoritesList);
        lv.setAdapter(sa);
        final DialogInterface di = this;
        lv.setOnItemClickListener(new OnItemClickListener() {
            @SuppressWarnings("unchecked")
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id) {
                HashMap<String, Object> m = (HashMap<String, Object>) sa.getItem(pos);

                favoriteName = (String) m.get("name");

                if (listener != null) {
                    listener.onClick(di, 1);
                }
            }
        });
    }
}
