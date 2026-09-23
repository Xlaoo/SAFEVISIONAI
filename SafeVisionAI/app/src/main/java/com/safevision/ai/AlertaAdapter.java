package com.safevision.ai;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


public class AlertaAdapter extends RecyclerView.Adapter<AlertaAdapter.ViewHolder> {


    private ArrayList<Alerta> lista;

    private OnAlertaClick listener;


    public interface OnAlertaClick {

        void onClick(Alerta alerta);

    }



    public AlertaAdapter(
            ArrayList<Alerta> lista,
            OnAlertaClick listener
    ){

        this.lista = lista;
        this.listener = listener;

    }




    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ){


        View vista = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_alerta,
                        parent,
                        false
                );


        return new ViewHolder(vista);

    }





    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ){


        Alerta alerta = lista.get(position);


        holder.titulo.setText(
                alerta.getTitulo()
        );


        holder.fecha.setText(
                alerta.getFecha()
        );


        holder.area.setText(
                alerta.getArea()
        );



        holder.itemView.setOnClickListener(v -> {

            listener.onClick(alerta);

        });


    }





    @Override
    public int getItemCount(){

        return lista.size();

    }





    public static class ViewHolder extends RecyclerView.ViewHolder{


        TextView titulo;
        TextView fecha;
        TextView area;

        ImageView imagen;



        public ViewHolder(@NonNull View itemView){

            super(itemView);


            titulo =
                    itemView.findViewById(
                            R.id.txtTituloAlerta
                    );


            fecha =
                    itemView.findViewById(
                            R.id.txtFechaAlerta
                    );


            area =
                    itemView.findViewById(
                            R.id.txtAreaAlerta
                    );


            imagen =
                    itemView.findViewById(
                            R.id.imgTrabajador
                    );


        }


    }


}