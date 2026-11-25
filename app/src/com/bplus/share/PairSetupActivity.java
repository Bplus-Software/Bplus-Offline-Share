package com.bplus.share;
import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
public class PairSetupActivity extends Activity {
 @Override protected void onCreate(Bundle b){super.onCreate(b);Ui.applyLocale(this);Ui.applyTheme(this);LinearLayout root=Ui.col(this);root.setBackgroundColor(Ui.bg);setContentView(root);PairUi.setup(this,root,this::finish);}
}
