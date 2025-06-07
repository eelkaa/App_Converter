package com.example.directcurrencyconverter.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.directcurrencyconverter.R
import com.example.directcurrencyconverter.data.local.CurrencyEntity

class CurrencySpinnerAdapter(
    context: Context,
    private val currencies: List<CurrencyEntity>
) : ArrayAdapter<CurrencyEntity>(context, 0, currencies) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_currency, parent, false)

        val currency = getItem(position) ?: return view

        val tvCurrencyName = view.findViewById<TextView>(R.id.tvCurrencyName)
        val tvCurrencyCode = view.findViewById<TextView>(R.id.tvCurrencyCode)
        val ivFlag = view.findViewById<ImageView>(R.id.ivFlag)

        tvCurrencyName.text = currency.code
        tvCurrencyCode.text = currency.currency

        val countryCode = currency.code.take(2).lowercase()
        Glide.with(context)
            .load("https://flagcdn.com/w80/$countryCode.png")
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .transition(DrawableTransitionOptions.withCrossFade(300))
            .placeholder(R.drawable.ic_default_flag)
            .error(R.drawable.ic_error_flag)
            .into(ivFlag)

        return view
    }
}