package br.gov.rs.procergs.prandroidlib.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import br.gov.rs.procergs.prandroidlib.R;
import br.gov.rs.procergs.prandroidlib.list.item.EntryItem;
import br.gov.rs.procergs.prandroidlib.list.item.Item;
import br.gov.rs.procergs.prandroidlib.list.item.SectionItem;

public class EntryAdapter extends ArrayAdapter<Item> {

	@SuppressWarnings("unused")
	private Context context;
	private ArrayList<Item> items;
	private LayoutInflater vi;
	
	public EntryAdapter(Context context, ArrayList<Item> items) {
		super(context,0, items);
		this.context = context;
		this.items = items;
		vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public ArrayList<Item> getItems() {
		return items;
	}

	/**
	 * Método que retorna a posição
	 * de um item de menu que não seja
	 * um item do tipo SectionItem
	 * @param item
	 * @return
	 */
	public int getItemPos(EntryItem item){
		final Item it = items.get(getPosition(item));
		
		int pos = 0;
		for (int i = 0; i < items.size(); i++) {
			if (it != null) {
				EntryItem ei = (EntryItem)it;
				pos = ei.indice;
				break;
			}
		}
		return pos;
	}
	
	@Override
	public int getCount() {
		return super.getCount();
	}

	@Override
	public View getView(int position, View v, ViewGroup parent) {
		
		final Item i = items.get(position);
		if (i != null) {
			if(i.isSection()){
				SectionItem si = (SectionItem)i;
				v = vi.inflate(R.layout.list_item_section, null);

				v.setOnClickListener(null);
				v.setOnLongClickListener(null);
				v.setLongClickable(false);
				
				final TextView sectionView = (TextView) v.findViewById(R.id.list_item_section_text);
				sectionView.setText(si.getTitle());
			}else{
				EntryItem ei = (EntryItem)i;
				v = vi.inflate(R.layout.list_item_entry, null);
				final ImageView icon = (ImageView)v.findViewById(R.id.list_item_entry_drawable);
				final TextView title = (TextView)v.findViewById(R.id.list_item_entry_title);
				final TextView subtitle = (TextView)v.findViewById(R.id.list_item_entry_summary);
				
				if (icon != null){
					icon.setImageResource(ei.resIcon);
				}
				
				if (ei.resIcon == 0){
					icon.setImageDrawable(ei.drawIcon);
				}
				
				if (title != null)
					title.setText(ei.title);
				
				if(subtitle != null)
					subtitle.setText(ei.subtitle);
			}
		}
		return v;
	}

}
