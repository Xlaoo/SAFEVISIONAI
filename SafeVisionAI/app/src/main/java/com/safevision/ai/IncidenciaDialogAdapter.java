package com.safevision.ai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class IncidenciaDialogAdapter extends RecyclerView.Adapter<IncidenciaDialogAdapter.ViewHolder> {

    private final List<TrabajadorReporte.IncidenciaItem> lista;

    public IncidenciaDialogAdapter(List<TrabajadorReporte.IncidenciaItem> lista) {
        this.lista = lista;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_incidencia_trabajador, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TrabajadorReporte.IncidenciaItem item = lista.get(position);
        holder.txtAccion.setText(item.getAccion() != null ? item.getAccion() : "Incidencia");
        holder.txtFecha.setText(item.getFechaTexto() != null ? item.getFechaTexto() : "");
        String obs = item.getObservacion();
        if (obs != null && !obs.trim().isEmpty()) {
            holder.txtObservacion.setVisibility(View.VISIBLE);
            holder.txtObservacion.setText("Observación: " + obs);
        } else {
            holder.txtObservacion.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return lista != null ? lista.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtAccion;
        TextView txtFecha;
        TextView txtObservacion;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAccion = itemView.findViewById(R.id.txtIncidenciaAccion);
            txtFecha = itemView.findViewById(R.id.txtIncidenciaFecha);
            txtObservacion = itemView.findViewById(R.id.txtIncidenciaObservacion);
        }
    }
}
