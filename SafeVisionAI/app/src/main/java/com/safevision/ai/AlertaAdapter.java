package com.safevision.ai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class AlertaAdapter
        extends RecyclerView.Adapter<AlertaAdapter.ViewHolder> {

    private ArrayList<Alerta> lista;

    private OnAlertaClick listener;

    private OnEliminarAlertaListener eliminarListener;


    // =========================================================
    // CLICK ALERTA
    // =========================================================

    public interface OnAlertaClick {

        void onClick(Alerta alerta);

    }


    // =========================================================
    // ELIMINAR ALERTA
    // =========================================================

    public interface OnEliminarAlertaListener {

        void onEliminar(Alerta alerta, int position);

    }


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public AlertaAdapter(
            ArrayList<Alerta> lista,
            OnAlertaClick listener,
            OnEliminarAlertaListener eliminarListener
    ) {

        this.lista = lista;

        this.listener = listener;

        this.eliminarListener = eliminarListener;
    }


    // =========================================================
    // CREAR VISTA
    // =========================================================

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View vista =
                LayoutInflater.from(parent.getContext())
                        .inflate(
                                R.layout.item_alerta,
                                parent,
                                false
                        );

        return new ViewHolder(vista);
    }


    // =========================================================
    // MOSTRAR ALERTA
    // =========================================================

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

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


        // =====================================================
        // ABRIR DETALLE
        // =====================================================

        holder.itemView.setOnClickListener(v -> {

            if (listener != null) {

                listener.onClick(alerta);

            }

        });


        // =====================================================
        // PAPELERA
        // =====================================================

        holder.btnEliminar.setOnClickListener(v -> {

            new AlertDialog.Builder(
                    holder.itemView.getContext()
            )

                    .setTitle("Eliminar alerta")

                    .setMessage(
                            "¿Deseas eliminar esta alerta de forma permanente?"
                    )

                    .setNegativeButton(
                            "Cancelar",
                            null
                    )

                    .setPositiveButton(
                            "Eliminar",
                            (dialog, which) -> {

                                if (eliminarListener != null) {

                                    eliminarListener.onEliminar(
                                            alerta,
                                            holder.getBindingAdapterPosition()
                                    );
                                }

                            }
                    )

                    .show();

        });

    }


    // =========================================================
    // CANTIDAD
    // =========================================================

    @Override
    public int getItemCount() {

        return lista.size();

    }


    // =========================================================
    // VIEW HOLDER
    // =========================================================

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        TextView titulo;

        TextView fecha;

        TextView area;

        ImageView imagen;

        ImageView btnEliminar;


        public ViewHolder(
                @NonNull View itemView
        ) {

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


            btnEliminar =
                    itemView.findViewById(
                            R.id.btnEliminarAlerta
                    );

        }
    }
}