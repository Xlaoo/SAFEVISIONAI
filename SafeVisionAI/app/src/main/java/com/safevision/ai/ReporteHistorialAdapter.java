package com.safevision.ai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ReporteHistorialAdapter extends RecyclerView.Adapter<ReporteHistorialAdapter.ViewHolder> {

    public interface OnVerReporteClickListener {
        void onVerClick(ReporteItem item);
    }

    private List<ReporteItem> lista;
    private final OnVerReporteClickListener listener;

    public ReporteHistorialAdapter(List<ReporteItem> lista, OnVerReporteClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    public void actualizarLista(List<ReporteItem> nuevaLista) {
        this.lista = nuevaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reporte_historial, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReporteItem item = lista.get(position);

        holder.txtFecha.setText("Fecha: " + item.getFecha());
        holder.txtHora.setText("Hora: " + item.getHora());
        holder.txtArea.setText(item.getArea());
        holder.txtCamara.setText(item.getCamara());
        holder.txtDescripcion.setText(item.getDescripcion());

        if (item.isRevisado()) {
            holder.txtEstadoBadge.setText("Revisado");
            holder.txtEstadoBadge.setBackgroundResource(R.drawable.bg_badge_revisado);
            holder.txtEstadoBadge.setTextColor(0xFF2E7D32);
        } else {
            holder.txtEstadoBadge.setText("Pendiente");
            holder.txtEstadoBadge.setBackgroundResource(R.drawable.bg_badge_pendiente);
            holder.txtEstadoBadge.setTextColor(0xFFE65100);
        }

        View.OnClickListener clickListener = v -> {
            if (listener != null) {
                listener.onVerClick(item);
            }
        };

        holder.btnVer.setOnClickListener(clickListener);
        holder.itemView.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return lista != null ? lista.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtFecha;
        TextView txtHora;
        TextView txtEstadoBadge;
        TextView txtArea;
        TextView txtCamara;
        TextView txtDescripcion;
        MaterialButton btnVer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtFecha = itemView.findViewById(R.id.txtFechaReporteItem);
            txtHora = itemView.findViewById(R.id.txtHoraReporteItem);
            txtEstadoBadge = itemView.findViewById(R.id.txtEstadoBadgeItem);
            txtArea = itemView.findViewById(R.id.txtAreaReporteItem);
            txtCamara = itemView.findViewById(R.id.txtCamaraReporteItem);
            txtDescripcion = itemView.findViewById(R.id.txtDescripcionReporteItem);
            btnVer = itemView.findViewById(R.id.btnVerReporteItem);
        }
    }
}
