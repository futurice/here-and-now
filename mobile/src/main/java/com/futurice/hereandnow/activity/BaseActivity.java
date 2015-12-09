package com.futurice.hereandnow.activity;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;

public class BaseActivity extends AppCompatActivity {
    protected boolean hasExtra(@NonNull @nonnull final String name) {
        return getIntent().getExtras() != null && getIntent().getExtras().get(name) != null;
    }

    @Nullable
    @nullable
    protected Object getExtra(@NonNull @nonnull final String name) {
        if (hasExtra(name)) {
            return getIntent().getExtras().get(name);
        }

        return null;
    }
}
