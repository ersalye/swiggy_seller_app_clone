package golhar.cocomo.zinger.seller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.Gson;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import golhar.cocomo.zinger.seller.adapter.OrderHistoryDetailAdapter;
import golhar.cocomo.zinger.seller.enums.UserRole;
import golhar.cocomo.zinger.seller.model.OrderItemListModel;
import golhar.cocomo.zinger.seller.model.OrderItemModel;
import golhar.cocomo.zinger.seller.model.OrderModel;
import golhar.cocomo.zinger.seller.service.MainRepository;
import golhar.cocomo.zinger.seller.utils.Constants;
import golhar.cocomo.zinger.seller.utils.ErrorLog;
import golhar.cocomo.zinger.seller.utils.Response;
import golhar.cocomo.zinger.seller.utils.SharedPref;
import retrofit2.Call;
import retrofit2.Callback;

public class OrderHistoryItemDetailActivity extends AppCompatActivity {

    List<OrderItemListModel> itemList;
    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy    hh:mm:ss a");
    TextView orderNumTV;
    TextView statusTV;
    TextView itemNumTV;
    TextView itemCostTV;
    TextView hotelTV;
    TextView collegeNameTV;
    TextView toTV;
    TextView toAddTV;
    TextView delDateTV;
    ListView itemsLV;
    TextView costTV;
    TextView deliveryCostTV;
    TextView totalCostTV;
    TextView viaTV;
    ImageButton backArrowIB;
    OrderHistoryDetailAdapter orderHistoryDetailAdapter;
    TextView lastUpdatedTimeTV;
    SwipeRefreshLayout pullToRefresh;
    OrderModel newOrderModel;
    OrderItemListModel orderItemListModel;
    Button reOrderBT;
    LinearLayout secretKeyLL;
    TextView secretKeyTV;
    String role;
    UserRole userRole;

    //todo ui fix remove delivery to

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history_item_detail);
        pullToRefresh = findViewById(R.id.pullToRefresh);
        Intent detail = getIntent();
        orderItemListModel = detail.getParcelableExtra("FullOrderDetails");
        secretKeyLL = findViewById(R.id.secretKeyLL);
        secretKeyTV = findViewById(R.id.secretKeyTV);
        backArrowIB = findViewById(R.id.backArrowIB);
        orderNumTV = findViewById(R.id.orderNumTV);
        statusTV = findViewById(R.id.statusTV);
        itemNumTV = findViewById(R.id.itemNumTV);
        itemCostTV = findViewById(R.id.itemCostTV);
        collegeNameTV = findViewById(R.id.collegeNameTV);
        toTV = findViewById(R.id.toTV);
        toAddTV = findViewById(R.id.toAddTV);
        costTV = findViewById(R.id.costTV);
        deliveryCostTV = findViewById(R.id.deliveryCostTV);
        totalCostTV = findViewById(R.id.totalCostTV);
        viaTV = findViewById(R.id.viaTV);
        itemsLV = findViewById(R.id.itemsLV);
        lastUpdatedTimeTV = findViewById(R.id.lastUpdatedTimeTV);
        List<OrderItemModel> orderItemList = orderItemListModel.getOrderItemsList();
        orderNumTV.setText("ORDER " + "#" + orderItemListModel.getOrderModel().getId());
        itemNumTV.setText(orderItemList.size() + "items");
        itemCostTV.setText("₹" + String.valueOf(orderItemListModel.getOrderModel().getPrice()));
        costTV.setText("₹" + String.valueOf(orderItemListModel.getOrderModel().getPrice() - orderItemListModel.getOrderModel().getDeliveryPrice()));
        toTV.setText(orderItemListModel.getOrderModel().getDeliveryLocation());
        toAddTV.setText(SharedPref.getString(getApplicationContext(), Constants.collegeName));
        deliveryCostTV.setText("₹" + String.valueOf(orderItemListModel.getOrderModel().getDeliveryPrice()));
        totalCostTV.setText("₹" + String.valueOf(orderItemListModel.getOrderModel().getPrice()));
        viaTV.setText("Paid Via " + orderItemListModel.getOrderModel().getTransactionModel().getPaymentMode());
        itemsLV.setScrollbarFadingEnabled(false);
        DateFormat df = new SimpleDateFormat("HH:mm:ss");
        Date date1 = orderItemListModel.getOrderModel().getLastStatusUpdatedTime();
        String status = String.valueOf(orderItemListModel.getOrderModel().getOrderStatus());
        statusChange(date1, status, orderItemListModel.getOrderModel().getDate());
        secretKeyLL.setVisibility(View.INVISIBLE);
        role = SharedPref.getString(getApplicationContext(), Constants.role);
        if (UserRole.SELLER.toString().equals(role)) {
            userRole = UserRole.SELLER;
        } else if (UserRole.SHOP_OWNER.toString().equals(role)) {
            userRole = UserRole.SHOP_OWNER;
        }

        backArrowIB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        orderHistoryDetailAdapter = new OrderHistoryDetailAdapter(this, R.layout.order_items, orderItemList);
        itemsLV.setAdapter(orderHistoryDetailAdapter);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getOrderById();
            }
        });


    }

    void getOrderById() {
        String phoneNo = SharedPref.getString(getApplicationContext(), Constants.phoneNumber);
        String authId = SharedPref.getString(getApplicationContext(), Constants.authId);
        MainRepository.getOrderService().getOrderById(orderItemListModel.getOrderModel().getId(), authId, phoneNo, userRole.name()).enqueue(new Callback<Response<OrderModel>>() {
            @Override
            public void onResponse(Call<Response<OrderModel>> call, retrofit2.Response<Response<OrderModel>> response) {
                Response<OrderModel> responseFromServer = response.body();
                if (responseFromServer.getCode().equals(ErrorLog.CodeSuccess) && responseFromServer.getMessage().equals(ErrorLog.Success)) {
                    newOrderModel = responseFromServer.getData();
                    statusChange(newOrderModel.getLastStatusUpdatedTime(), newOrderModel.getOrderStatus().toString(), newOrderModel.getDate());
                    if (newOrderModel.getSecretKey() != null) {
                        secretKeyLL.setVisibility(View.VISIBLE);
                        secretKeyTV.setText(newOrderModel.getSecretKey());
                    } else {
                        secretKeyLL.setVisibility(View.INVISIBLE);
                    }
                    pullToRefresh.setRefreshing(false);
                } else {
                    pullToRefresh.setRefreshing(false);
                }
            }

            @Override
            public void onFailure(Call<Response<OrderModel>> call, Throwable t) {
                Toast.makeText(OrderHistoryItemDetailActivity.this, "Failure" + t.getMessage(), Toast.LENGTH_SHORT).show();
                pullToRefresh.setRefreshing(false);
            }
        });
    }

    void statusChange(Date date1, String status, Date getDate) {
        if (date1 != null) {
            Calendar c = Calendar.getInstance();
            Date date2 = c.getTime();
            long diff = date2.getTime() - date1.getTime();
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;
            if (days > 0) {
                lastUpdatedTimeTV.setText("Lasted updated " + days + " day ago");
            } else if (minutes < 60) {
                lastUpdatedTimeTV.setText("Lasted updated " + minutes + " minutes ago");
            } else if (hours == 1) {
                lastUpdatedTimeTV.setText("Lasted updated an hour ago");
            } else {
                lastUpdatedTimeTV.setText(date1.toString());
            }
        } else {
            lastUpdatedTimeTV.setVisibility(View.INVISIBLE);
        }
        statusTV.setText(status);
        switch (status) {
            case "PENDING":
                statusTV.setTextColor(Color.parseColor("#006400"));
                lastUpdatedTimeTV.setVisibility(View.VISIBLE);
                date1 = getDate;
                if (date1 != null) {
                    Calendar c = Calendar.getInstance();
                    Date date2 = c.getTime();
                    long diff = date2.getTime() - date1.getTime();
                    long seconds = diff / 1000;
                    long minutes = seconds / 60;
                    long hours = minutes / 60;
                    long days = hours / 24;
                    if (minutes < 60) {
                        lastUpdatedTimeTV.setText("Lasted updated " + minutes + " minutes ago");
                    } else if (hours == 1) {
                        lastUpdatedTimeTV.setText("Lasted updated an hour ago");
                    } else {
                        String dayOfTheWeek = (String) android.text.format.DateFormat.format("EEEE", date1); // Thursday
                        String day = (String) android.text.format.DateFormat.format("dd", date1); // 20
                        String monthString = (String) android.text.format.DateFormat.format("MMM", date1); // Jun
                        String monthNumber = (String) android.text.format.DateFormat.format("MM", date1); // 06
                        String year = (String) android.text.format.DateFormat.format("yyyy", date1); // 2013
                        String timehh = (String) android.text.format.DateFormat.format("hh", date1);
                        String timemm = (String) android.text.format.DateFormat.format("mm", date1);
                        String ampm="am";
                        if(Integer.parseInt(timehh)>12){
                            ampm="pm";
                        }
                        lastUpdatedTimeTV.setText("Lasted updated on " + monthString + " " + day + " " + timehh+":"+timemm+ampm);
                    }
                }
                break;
            case "TXN_FAILURE":
                statusTV.setTextColor(Color.parseColor("#FF0000"));
                lastUpdatedTimeTV.setVisibility(View.VISIBLE);
                date1 = getDate;
                if (date1 != null) {
                    Calendar c = Calendar.getInstance();
                    Date date2 = c.getTime();
                    long diff = date2.getTime() - date1.getTime();
                    long seconds = diff / 1000;
                    long minutes = seconds / 60;
                    long hours = minutes / 60;
                    long days = hours / 24;
                    if (minutes < 60) {
                        lastUpdatedTimeTV.setText("Lasted updated " + minutes + " minutes ago");
                    } else if (hours == 1) {
                        lastUpdatedTimeTV.setText("Lasted updated an hour ago");
                    } else {
                        String dayOfTheWeek = (String) android.text.format.DateFormat.format("EEEE", date1); // Thursday
                        String day = (String) android.text.format.DateFormat.format("dd", date1); // 20
                        String monthString = (String) android.text.format.DateFormat.format("MMM", date1); // Jun
                        String monthNumber = (String) android.text.format.DateFormat.format("MM", date1); // 06
                        String year = (String) android.text.format.DateFormat.format("yyyy", date1); // 2013
                        String timehh = (String) android.text.format.DateFormat.format("hh", date1);
                        String timemm = (String) android.text.format.DateFormat.format("mm", date1);
                        String ampm="am";
                        if(Integer.parseInt(timehh)>12){
                            ampm="pm";
                        }
                        lastUpdatedTimeTV.setText("Lasted updated on " + monthString + " " + day + " " + timehh+":"+timemm+ampm);
                    }
                }
                break;
            case "PLACED":
                statusTV.setTextColor(Color.parseColor("#00FF00"));
                lastUpdatedTimeTV.setVisibility(View.VISIBLE);
                date1 = getDate;
                if (date1 != null) {
                    Calendar c = Calendar.getInstance();
                    Date date2 = c.getTime();
                    long diff = date2.getTime() - date1.getTime();
                    long seconds = diff / 1000;
                    long minutes = seconds / 60;
                    long hours = minutes / 60;
                    long days = hours / 24;
                    if (minutes < 60) {
                        lastUpdatedTimeTV.setText("Lasted updated " + minutes + " minutes ago");
                    } else if (hours == 1) {
                        lastUpdatedTimeTV.setText("Lasted updated an hour ago");
                    } else {
                        String dayOfTheWeek = (String) android.text.format.DateFormat.format("EEEE", date1); // Thursday
                        String day = (String) android.text.format.DateFormat.format("dd", date1); // 20
                        String monthString = (String) android.text.format.DateFormat.format("MMM", date1); // Jun
                        String monthNumber = (String) android.text.format.DateFormat.format("MM", date1); // 06
                        String year = (String) android.text.format.DateFormat.format("yyyy", date1); // 2013
                        String timehh = (String) android.text.format.DateFormat.format("hh", date1);
                        String timemm = (String) android.text.format.DateFormat.format("mm", date1);
                        String ampm="am";
                        if(Integer.parseInt(timehh)>12){
                            ampm="pm";
                        }
                        lastUpdatedTimeTV.setText("Lasted updated on " + monthString + " " + day + " " + timehh+":"+timemm+ampm);
                    }
                }
                break;
            case "CANCELLED_BY_USER":
                statusTV.setTextColor(Color.parseColor("#FF0000"));
                lastUpdatedTimeTV.setVisibility(View.VISIBLE);
                date1 = getDate;
                if (date1 != null) {
                    Calendar c = Calendar.getInstance();
                    Date date2 = c.getTime();
                    long diff = date2.getTime() - date1.getTime();
                    long seconds = diff / 1000;
                    long minutes = seconds / 60;
                    long hours = minutes / 60;
                    long days = hours / 24;
                    if (minutes < 60) {
                        lastUpdatedTimeTV.setText("Lasted updated " + minutes + " minutes ago");
                    } else if (hours == 1) {
                        lastUpdatedTimeTV.setText("Lasted updated an hour ago");
                    } else {
                        String dayOfTheWeek = (String) android.text.format.DateFormat.format("EEEE", date1); // Thursday
                        String day = (String) android.text.format.DateFormat.format("dd", date1); // 20
                        String monthString = (String) android.text.format.DateFormat.format("MMM", date1); // Jun
                        String monthNumber = (String) android.text.format.DateFormat.format("MM", date1); // 06
                        String year = (String) android.text.format.DateFormat.format("yyyy", date1); // 2013
                        String timehh = (String) android.text.format.DateFormat.format("hh", date1);
                        String timemm = (String) android.text.format.DateFormat.format("mm", date1);
                        String ampm="am";
                        if(Integer.parseInt(timehh)>12){
                            ampm="pm";
                        }
                        lastUpdatedTimeTV.setText("Lasted updated on " + monthString + " " + day + " " + timehh+":"+timemm+ampm);
                    }
                }
                break;
            case "ACCEPTED":
                statusTV.setTextColor(Color.parseColor("#e25822"));
                lastUpdatedTimeTV.setVisibility(View.VISIBLE);
                date1 = getDate;
                if (date1 != null) {
                    Calendar c = Calendar.getInstance();
                    Date date2 = c.getTime();
                    long diff = date2.getTime() - date1.getTime();
                    long seconds = diff / 1000;
                    long minutes = seconds / 60;
                    long hours = minutes / 60;
                    long days = hours / 24;
                    if (minutes < 60) {
                        lastUpdatedTimeTV.setText("Lasted updated " + minutes + " minutes ago");
                    } else if (hours == 1) {
                        lastUpdatedTimeTV.setText("Lasted updated an hour ago");
                    } else {
                        String dayOfTheWeek = (String) android.text.format.DateFormat.format("EEEE", date1); // Thursday
                        String day = (String) android.text.format.DateFormat.format("dd", date1); // 20
                        String monthString = (String) android.text.format.DateFormat.format("MMM", date1); // Jun
                        String monthNumber = (String) android.text.format.DateFormat.format("MM", date1); // 06
                        String year = (String) android.text.format.DateFormat.format("yyyy", date1); // 2013
                        String timehh = (String) android.text.format.DateFormat.format("hh", date1);
                        String timemm = (String) android.text.format.DateFormat.format("mm", date1);
                        String ampm="am";
                        if(Integer.parseInt(timehh)>12){
                            ampm="pm";
                        }
                        lastUpdatedTimeTV.setText("Lasted updated on " + monthString + " " + day + " " + timehh+":"+timemm+ampm);
                    }
                }
                break;
            case "CANCELLED_BY_SELLER":
                statusTV.setTextColor(Color.parseColor("#FF0000"));
                lastUpdatedTimeTV.setVisibility(View.VISIBLE);
                date1 = getDate;
                if (date1 != null) {
                    Calendar c = Calendar.getInstance();
                    Date date2 = c.getTime();
                    long diff = date2.getTime() - date1.getTime();
                    long seconds = diff / 1000;
                    long minutes = seconds / 60;
                    long hours = minutes / 60;
                    long days = hours / 24;
                    if (minutes < 60) {
                        lastUpdatedTimeTV.setText("Lasted updated " + minutes + " minutes ago");
                    } else if (hours == 1) {
                        lastUpdatedTimeTV.setText("Lasted updated an hour ago");
                    } else {
                        String dayOfTheWeek = (String) android.text.format.DateFormat.format("EEEE", date1); // Thursday
                        String day = (String) android.text.format.DateFormat.format("dd", date1); // 20
                        String monthString = (String) android.text.format.DateFormat.format("MMM", date1); // Jun
                        String monthNumber = (String) android.text.format.DateFormat.format("MM", date1); // 06
                        String year = (String) android.text.format.DateFormat.format("yyyy", date1); // 2013
                        String timehh = (String) android.text.format.DateFormat.format("hh", date1);
                        String timemm = (String) android.text.format.DateFormat.format("mm", date1);
                        String ampm="am";
                        if(Integer.parseInt(timehh)>12){
                            ampm="pm";
                        }
                        lastUpdatedTimeTV.setText("Lasted updated on " + monthString + " " + day + " " + timehh+":"+timemm+ampm);
                    }
                }
                break;
            case "READY":
                statusTV.setTextColor(Color.parseColor("#e25822"));
                lastUpdatedTimeTV.setVisibility(View.VISIBLE);
                date1 = getDate;
                if (date1 != null) {
                    Calendar c = Calendar.getInstance();
                    Date date2 = c.getTime();
                    long diff = date2.getTime() - date1.getTime();
                    long seconds = diff / 1000;
                    long minutes = seconds / 60;
                    long hours = minutes / 60;
                    long days = hours / 24;
                    if (minutes < 60) {
                        lastUpdatedTimeTV.setText("Lasted updated " + minutes + " minutes ago");
                    } else if (hours == 1) {
                        lastUpdatedTimeTV.setText("Lasted updated an hour ago");
                    } else {
                        String dayOfTheWeek = (String) android.text.format.DateFormat.format("EEEE", date1); // Thursday
                        String day = (String) android.text.format.DateFormat.format("dd", date1); // 20
                        String monthString = (String) android.text.format.DateFormat.format("MMM", date1); // Jun
                        String monthNumber = (String) android.text.format.DateFormat.format("MM", date1); // 06
                        String year = (String) android.text.format.DateFormat.format("yyyy", date1); // 2013
                        String timehh = (String) android.text.format.DateFormat.format("hh", date1);
                        String timemm = (String) android.text.format.DateFormat.format("mm", date1);
                        String ampm="am";
                        if(Integer.parseInt(timehh)>12){
                            ampm="pm";
                        }
                        lastUpdatedTimeTV.setText("Lasted updated on " + monthString + " " + day + " " + timehh+":"+timemm+ampm);
                    }
                }
                break;
            case "OUT_FOR_DELIVERY":
                statusTV.setTextColor(Color.parseColor("#e25822"));
                lastUpdatedTimeTV.setVisibility(View.VISIBLE);
                date1 = getDate;
                if (date1 != null) {
                    Calendar c = Calendar.getInstance();
                    Date date2 = c.getTime();
                    long diff = date2.getTime() - date1.getTime();
                    long seconds = diff / 1000;
                    long minutes = seconds / 60;
                    long hours = minutes / 60;
                    long days = hours / 24;
                    if (minutes < 60) {
                        lastUpdatedTimeTV.setText("Lasted updated " + minutes + " minutes ago");
                    } else if (hours == 1) {
                        lastUpdatedTimeTV.setText("Lasted updated an hour ago");
                    } else {
                        String dayOfTheWeek = (String) android.text.format.DateFormat.format("EEEE", date1); // Thursday
                        String day = (String) android.text.format.DateFormat.format("dd", date1); // 20
                        String monthString = (String) android.text.format.DateFormat.format("MMM", date1); // Jun
                        String monthNumber = (String) android.text.format.DateFormat.format("MM", date1); // 06
                        String year = (String) android.text.format.DateFormat.format("yyyy", date1); // 2013
                        String timehh = (String) android.text.format.DateFormat.format("hh", date1);
                        String timemm = (String) android.text.format.DateFormat.format("mm", date1);
                        String ampm="am";
                        if(Integer.parseInt(timehh)>12){
                            ampm="pm";
                        }
                        lastUpdatedTimeTV.setText("Lasted updated on " + monthString + " " + day + " " + timehh+":"+timemm+ampm);
                    }
                }
                break;
            case "COMPLETED":
                statusTV.setTextColor(Color.parseColor("#00FF00"));
                lastUpdatedTimeTV.setVisibility(View.VISIBLE);
                date1 = getDate;
                if (date1 != null) {
                    Calendar c = Calendar.getInstance();
                    Date date2 = c.getTime();
                    long diff = date2.getTime() - date1.getTime();
                    long seconds = diff / 1000;
                    long minutes = seconds / 60;
                    long hours = minutes / 60;
                    long days = hours / 24;
                    if (minutes < 60) {
                        lastUpdatedTimeTV.setText("Lasted updated " + minutes + " minutes ago");
                    } else if (hours == 1) {
                        lastUpdatedTimeTV.setText("Lasted updated an hour ago");
                    } else {
                        String dayOfTheWeek = (String) android.text.format.DateFormat.format("EEEE", date1); // Thursday
                        String day = (String) android.text.format.DateFormat.format("dd", date1); // 20
                        String monthString = (String) android.text.format.DateFormat.format("MMM", date1); // Jun
                        String monthNumber = (String) android.text.format.DateFormat.format("MM", date1); // 06
                        String year = (String) android.text.format.DateFormat.format("yyyy", date1); // 2013
                        String timehh = (String) android.text.format.DateFormat.format("hh", date1);
                        String timemm = (String) android.text.format.DateFormat.format("mm", date1);
                        String ampm="am";
                        if(Integer.parseInt(timehh)>12){
                            ampm="pm";
                        }
                        lastUpdatedTimeTV.setText("Lasted updated on " + monthString + " " + day + " " + timehh+":"+timemm+ampm);
                    }
                }
                break;
            case "DELIVERED":
                statusTV.setTextColor(Color.parseColor("#00FF00"));
                lastUpdatedTimeTV.setVisibility(View.VISIBLE);
                date1 = getDate;
                if (date1 != null) {
                    Calendar c = Calendar.getInstance();
                    Date date2 = c.getTime();
                    long diff = date2.getTime() - date1.getTime();
                    long seconds = diff / 1000;
                    long minutes = seconds / 60;
                    long hours = minutes / 60;
                    long days = hours / 24;
                    if (minutes < 60) {
                        lastUpdatedTimeTV.setText("Lasted updated " + minutes + " minutes ago");
                    } else if (hours == 1) {
                        lastUpdatedTimeTV.setText("Lasted updated an hour ago");
                    } else {
                        String dayOfTheWeek = (String) android.text.format.DateFormat.format("EEEE", date1); // Thursday
                        String day = (String) android.text.format.DateFormat.format("dd", date1); // 20
                        String monthString = (String) android.text.format.DateFormat.format("MMM", date1); // Jun
                        String monthNumber = (String) android.text.format.DateFormat.format("MM", date1); // 06
                        String year = (String) android.text.format.DateFormat.format("yyyy", date1); // 2013
                        String timehh = (String) android.text.format.DateFormat.format("hh", date1);
                        String timemm = (String) android.text.format.DateFormat.format("mm", date1);
                        String ampm="am";
                        if(Integer.parseInt(timehh)>12){
                            ampm="pm";
                        }
                        lastUpdatedTimeTV.setText("Lasted updated on " + monthString + " " + day + " " + timehh+":"+timemm+ampm);
                    }
                }
                break;
        }
    }

    void LoadData(ArrayList<OrderItemModel> orderItemModelArrayList) {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(Constants.sharedPreferencesCart, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(orderItemModelArrayList);
        editor.putString(Constants.cart, json);
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
