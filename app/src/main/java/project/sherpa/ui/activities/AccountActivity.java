package project.sherpa.ui.activities;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import project.sherpa.R;
import project.sherpa.databinding.ActivityAccountBinding;
import project.sherpa.models.viewmodels.AccountViewModel;

/**
 * Created by Alvin on 7/28/2017.
 */

public class AccountActivity extends AppCompatActivity {
    // ** Member Variables ** //
    ActivityAccountBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_account);
        AccountViewModel vm = new AccountViewModel(this);
        mBinding.setVm(vm);
    }
}
