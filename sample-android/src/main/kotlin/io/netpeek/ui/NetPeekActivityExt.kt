package io.netpeek.ui

import android.content.Context
import android.content.Intent

fun NetPeekActivity.Companion.newIntent(context: Context): Intent =
    Intent(context, NetPeekActivity::class.java)
