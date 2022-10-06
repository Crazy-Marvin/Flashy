package rocks.poopjournal.flashy;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.widget.RelativeLayout;

public class DialogsUtil {




    public static Dialog showNoFlashLightDialog(Activity activity){

        Dialog dialog;
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_no_flashlight);

        dialog.getWindow().getAttributes().gravity = Gravity.BOTTOM;
        dialog.getWindow().getAttributes().verticalMargin = 0;
        dialog.getWindow().getAttributes().horizontalMargin=0;
        dialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(activity.getResources().getColor(R.color.colorDialogTransclucent)));
        dialog.getWindow().setLayout(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        return dialog;

    }

}
