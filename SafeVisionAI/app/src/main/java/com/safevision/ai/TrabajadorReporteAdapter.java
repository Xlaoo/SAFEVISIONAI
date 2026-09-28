package com.safevision.ai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class TrabajadorReporteAdapter extends RecyclerView.Adapter<TrabajadorReporteAdapter.ViewHolder> {

    public interface OnVerClickListener {
        void onVerClick(TrabajadorReporte trabajador);
    }

    private List<TrabajadorReporte> lista;
    private final OnVerClickListener listener;

    public TrabajadorReporteAdapter(List<TrabajadorReporte> lista, OnVerClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    public void actualizarLista(List<TrabajadorReporte> nuevaLista) {
        this.lista = nuevaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trabajador_reporte, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TrabajadorReporte item = lista.get(position);

        holder.txtNombre.setText(item.getNombreCompleto());
        holder.txtArea.setText(item.getArea());
        holder.txtVecesReportado.setText(String.valueOf(item.getVecesReportado()));
        holder.txtUltimoReporte.setText(item.getUltimoReporteTexto());

        holder.btnVer.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVerClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return lista != null ? lista.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre;
        TextView txtArea;
        TextView txtVecesReportado;
        TextView txtUltimoReporte;
        MaterialButton btnVer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNombre = itemView.findViewById(R.id.txtNombreTrabajadorReporte);
            txtArea = itemView.findViewById(R.id.txtAreaTrabajadorReporte);
            txtVecesReportado = itemView.findViewById(R.id.txtVecesReportado);
            txtUltimoReporte = itemView.findViewById(R.id.txtUltimoReporte);
            btnVer = itemView.findViewById(R.id.btnVerTrabajadorReporte);
        }
    }
}
