
package storeaui.widget.itemview;

import storeaui.widget.item.Item;
import storeaui.widget.item.TextItem;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * View representation of the {@link TextItem}.
 *
 */
public class TextItemView extends TextView implements ItemView{

    public TextItemView(Context context) {
        this(context, null);
    }

    public TextItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void prepareItemView() {
    }

    public void setObject(Item object) {
        this.setText(((TextItem)object).mText);
        this.setEnabled(object.isEnabled());
    }

    @Override
    public void setSubTextSingleLine(boolean enabled) {
    }
}
