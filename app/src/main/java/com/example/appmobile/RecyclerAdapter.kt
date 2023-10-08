package com.example.appmobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView

internal class RecyclerAdapter(private var listNotificacoes: MutableList<NotificacoesModel> = mutableListOf<NotificacoesModel>()) :
    RecyclerView.Adapter<RecyclerAdapter.MyViewHolder>() {
    internal inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var title: TextView = view.findViewById(R.id.titleNotify)
        var desc: TextView = view.findViewById(R.id.descNotify)
        var icon: ImageView = view.findViewById(R.id.iconNotify)
    }
    @NonNull
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_item_row, parent, false)
        return MyViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = listNotificacoes[position].titulo
        holder.title.text = item
        val itemDesc = listNotificacoes[position].descricao
        holder.desc.text = itemDesc
        val itemType = listNotificacoes[position].ntipo
        when(itemType){
            1 -> holder.icon.setImageResource(R.drawable.info_svg)
            2 -> holder.icon.setImageResource(R.drawable.atencao_svg)
            3 -> holder.icon.setImageResource(R.drawable.perigo_svg)
            4 -> holder.icon.setImageResource(R.drawable.maintenance_svg)
        }
    }
    override fun getItemCount(): Int {
        return listNotificacoes.size
    }

    fun removeAt(position: Int) {
        listNotificacoes.removeAt(position)
        notifyItemRemoved(position)
    }

    fun addAt(position : Int, item : NotificacoesModel){
        val removedPosition = position

        listNotificacoes.add(removedPosition, item)
        notifyItemInserted(removedPosition)
    }

    fun getPosList(position: Int) : Int{
        return listNotificacoes[position].nnotificacao
    }

}