package com.example.ibnsina;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {
    private List<InventoryModel> list;
    private Context context;
    private DatabaseHelper dbHelper;
    private long lastClickTime = 0;

    public InventoryAdapter(List<InventoryModel> list) {
        this.list = list;
    }

    public void updateList(List<InventoryModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        dbHelper = new DatabaseHelper(context);
        View view = LayoutInflater.from(context).inflate(R.layout.item_inventory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryModel model = list.get(position);

        if (holder.tvSl != null) holder.tvSl.setText(model.getSl());
        if (holder.tvCategory != null) holder.tvCategory.setText(model.getCategory());
        if (holder.tvProductName != null) holder.tvProductName.setText(model.getProductName());
        if (holder.tvCode != null) holder.tvCode.setText(model.getCode());
        if (holder.tvPackSize != null) holder.tvPackSize.setText("Pack size: " + model.getPackSize());
        
        updateCalculatedFields(holder, model);

        holder.etShortQty.setText(model.getShortQty());
        holder.etExcessQty.setText(model.getExcessQty());
        holder.etRemark.setText(model.getRemark());

        if ("Checked".equalsIgnoreCase(model.getStatus())) {
            holder.itemContainer.setBackgroundColor(Color.parseColor("#C8E6C9"));
            holder.btnCheckUpdate.setChecked(true);
        } else {
            holder.itemContainer.setBackgroundColor(Color.WHITE);
            holder.btnCheckUpdate.setChecked(false);
        }

        holder.tvCartonSize.setOnClickListener(v -> {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTime < 350) {
                showEditCartonSizeDialog(model, holder);
            }
            lastClickTime = clickTime;
        });

        holder.btnCheckUpdate.setOnClickListener(v -> {
            if (context instanceof MainActivity && ((MainActivity) context).isLoading()) {
                holder.btnCheckUpdate.setChecked(!holder.btnCheckUpdate.isChecked());
                return;
            }

            if (holder.btnCheckUpdate.isChecked()) {
                model.setStatus("Checked");
                holder.itemContainer.setBackgroundColor(Color.parseColor("#C8E6C9"));
                
                model.setShortQty(holder.etShortQty.getText().toString());
                model.setExcessQty(holder.etExcessQty.getText().toString());
                model.setRemark(holder.etRemark.getText().toString());

                sendData(model.getCode(), model.getShortQty(), model.getExcessQty(), model.getRemark(), "Checked");
                if (context instanceof MainActivity) ((MainActivity) context).updateCheckedCount();
            } else {
                holder.btnCheckUpdate.setChecked(true);
                Toast.makeText(context, "Long press to uncheck", Toast.LENGTH_SHORT).show();
            }
        });

        holder.itemContainer.setOnLongClickListener(v -> {
            if (context instanceof MainActivity && ((MainActivity) context).isLoading()) return true;
            if ("Checked".equalsIgnoreCase(model.getStatus())) {
                AlertDialog dialog = new AlertDialog.Builder(context)
                        .setTitle("Uncheck Item?").setMessage("Do you want to uncheck this item?")
                        .setPositiveButton("Yes", (di, w) -> {
                            model.setStatus("Unchecked");
                            holder.btnCheckUpdate.setChecked(false);
                            holder.itemContainer.setBackgroundColor(Color.WHITE);
                            sendData(model.getCode(), "", "", "", "Unchecked");
                            if (context instanceof MainActivity) ((MainActivity) context).updateCheckedCount();
                        }).setNegativeButton("No", null).show();
                
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.parseColor("#4CAF50"));
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.parseColor("#F44336"));
                return true;
            }
            return false;
        });
    }

    private void updateCalculatedFields(ViewHolder holder, InventoryModel model) {
        try {
            int totalStock = Integer.parseInt(model.getTotalQty().trim());
            int cartonSize = Integer.parseInt(model.getCartonSize().trim());
            int calculatedCartonQty = (cartonSize > 0) ? totalStock / cartonSize : 0;
            int calculatedLooseQty = (cartonSize > 0) ? totalStock % cartonSize : totalStock;

            holder.tvTotalQty.setText("Total Stock: " + totalStock);
            holder.tvCartonSize.setText("Crton Size: " + cartonSize);
            holder.tvCarton.setText("Carton Qty: " + calculatedCartonQty);
            holder.tvLoose.setText("Loose Qty: " + calculatedLooseQty);
        } catch (Exception e) {
            holder.tvCarton.setText("Carton Qty: 0");
            holder.tvLoose.setText("Loose Qty: 0");
        }
    }

    private void showEditCartonSizeDialog(InventoryModel model, ViewHolder holder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Update Carton Size for " + model.getProductName());
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(model.getCartonSize());
        builder.setView(input);
        builder.setPositiveButton("Update", (dialog, which) -> {
            String newSize = input.getText().toString().trim();
            if (!newSize.isEmpty()) {
                model.setCartonSize(newSize);
                updateCalculatedFields(holder, model);
                updateCartonSizeOnServer(model.getCode(), newSize);
            }
        }).setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.parseColor("#2196F3"));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.GRAY);
    }

    private void updateCartonSizeOnServer(String code, String newSize) {
        if (context instanceof MainActivity) ((MainActivity) context).setLoading(true);
        try {
            String url = Config.SCRIPT_URL + "?action=updateCartonSize&code=" + URLEncoder.encode(code, "UTF-8") + "&cartonSize=" + newSize;
            Volley.newRequestQueue(context).add(new StringRequest(Request.Method.GET, url, res -> {
                if (context instanceof MainActivity) {
                    ((MainActivity) context).setLoading(false);
                    ((MainActivity) context).showBigSuccessDialog();
                }
            }, err -> { if (context instanceof MainActivity) ((MainActivity) context).setLoading(false); }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void sendData(String code, String s, String e, String r, String status) {
        if (context instanceof MainActivity) ((MainActivity) context).setLoading(true);
        
        // সেশন থেকে ইউজার আইডি ও নাম নেওয়া
        SharedPreferences prefs = context.getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", "Unknown");
        String userName = prefs.getString("userName", "Unknown");

        try {
            String url = Config.SCRIPT_URL + "?action=updateStock"
                    + "&code=" + URLEncoder.encode(code, "UTF-8")
                    + "&shortQty=" + URLEncoder.encode(s, "UTF-8")
                    + "&excessQty=" + URLEncoder.encode(e, "UTF-8")
                    + "&remark=" + URLEncoder.encode(r, "UTF-8")
                    + "&status=" + URLEncoder.encode(status, "UTF-8")
                    + "&userId=" + URLEncoder.encode(userId, "UTF-8")
                    + "&userName=" + URLEncoder.encode(userName, "UTF-8");

            Volley.newRequestQueue(context).add(new StringRequest(Request.Method.GET, url,
                    res -> {
                        if (context instanceof MainActivity) {
                            ((MainActivity) context).setLoading(false);
                            ((MainActivity) context).showBigSuccessDialog();
                        }
                        dbHelper.deleteUpdate(code);
                    },
                    err -> {
                        if (context instanceof MainActivity) ((MainActivity) context).setLoading(false);
                        dbHelper.addUpdate(code, s, e, r, status);
                    }));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @Override public int getItemCount() { return list == null ? 0 : list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSl, tvCategory, tvPackSize, tvCode, tvProductName, tvTotalQty, tvLoose, tvCarton, tvCartonSize;
        EditText etShortQty, etExcessQty, etRemark;
        CheckBox btnCheckUpdate;
        LinearLayout itemContainer;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSl = itemView.findViewById(R.id.tvSl);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvPackSize = itemView.findViewById(R.id.tvPackSize);
            tvCode = itemView.findViewById(R.id.tvCode);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvTotalQty = itemView.findViewById(R.id.tvTotalQty);
            tvLoose = itemView.findViewById(R.id.tvLoose);
            tvCarton = itemView.findViewById(R.id.tvCarton);
            tvCartonSize = itemView.findViewById(R.id.tvCartonSize);
            etShortQty = itemView.findViewById(R.id.etShortQty);
            etExcessQty = itemView.findViewById(R.id.etExcessQty);
            etRemark = itemView.findViewById(R.id.etRemark);
            btnCheckUpdate = itemView.findViewById(R.id.btnCheckUpdate);
            itemContainer = itemView.findViewById(R.id.itemContainer);
        }
    }
}