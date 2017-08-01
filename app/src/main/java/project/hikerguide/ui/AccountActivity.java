package project.hikerguide.ui;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import project.hikerguide.R;
import project.hikerguide.databinding.ActivityAccountBinding;
import project.hikerguide.models.viewmodels.AccountViewModel;

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
