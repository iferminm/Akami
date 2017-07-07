package com.jiahaoliuliu.akami.transactionslist;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jiahaoliuliu.akami.R;
import com.jiahaoliuliu.akami.model.Expense;
import com.jiahaoliuliu.akami.model.Sms;
import com.jiahaoliuliu.akami.model.ITransactions;
import com.jiahaoliuliu.akami.model.Withdraw;
import com.jiahaoliuliu.akami.modelviewpresenter.BaseActivity;
import com.jiahaoliuliu.akami.modelviewpresenter.BasePresenter;
import com.jiahaoliuliu.akami.modelviewpresenter.BaseView;
import com.jiahaoliuliu.akami.ui.MonthlyTransactionsActivity;
import com.jiahaoliuliu.akami.ui.TransactionsListAdapter;
import com.jiahaoliuliu.akami.utils.HeaderUtility;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity
        implements TransactionsListContract.View, BaseView {

    private static final String TAG = "MainActivity";
    private static final int MENU_ITEM_SHOW_MONTHLY_GRAPH_ID = 1000;

    // Date to be displayed as header
    private static final String HEADER_DATE_FORMAT = "MMMM yyyy";

    // Views
    private LinearLayout mHeaderLinearLayout;
    private TextView mHeaderDateTextView;
    private TextView mHeaderQuantityTextView;
    private RecyclerView mTransactionsRecyclerView;
    private TextView mNoSmsTextView;

    // The expenses per month
    private HashMap<Long, Float> mTransactionsPerMonth;
    private List<ITransactions> mTransactionsList;
    private TransactionsListAdapter mTransactionsListAdapter;
    private LinearLayoutManager mLinearLayoutManager;

    // The header date formatter. This has to be static in order to be used by the adapter
    public static SimpleDateFormat sHeaderDateFormatter = new SimpleDateFormat(HEADER_DATE_FORMAT);
    // The month of the first element shown in the header
    private long mFirstElementMonthlyKey;

    private TransactionsListPresenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set the base presenter for the base view and for this view
        // This should be done before the method onCreated because
        // it is linked on the lifecycle of the application
        mPresenter = (TransactionsListPresenter) getPresenter();
        setPresenter(mPresenter);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        linkViews();
        setViewLogic();

        getContentResolver();
        // Parse the list of transactions from the device
        parseTransactions();
        showTransactionsList();
    }

    private void linkViews() {
        mHeaderDateTextView = (TextView) findViewById(R.id.header_date_text_view);
        mHeaderQuantityTextView = (TextView) findViewById(R.id.header_quantity_text_view);

        mTransactionsRecyclerView = (RecyclerView) findViewById(R.id.transactions_recycler_view);
        mTransactionsRecyclerView.setHasFixedSize(true);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mTransactionsRecyclerView.setLayoutManager(mLinearLayoutManager);

        mNoSmsTextView = (TextView) findViewById(R.id.no_sms_text_view);
    }

    private void setViewLogic() {
        // Set the logic for the recycler view
        mTransactionsRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int firstElementPosition = mLinearLayoutManager.findFirstVisibleItemPosition();
                ITransactions firstTransaction = mTransactionsList.get(firstElementPosition);

                // Update the header if needed
                long currentMonthlyKey = HeaderUtility.getHeaderMonthlyKeyByTransaction(firstTransaction);
                if (currentMonthlyKey != mFirstElementMonthlyKey) {
                    // Update the month
                    mHeaderDateTextView.setText(sHeaderDateFormatter.format(firstTransaction.getDate()));

                    // Update the quantity
                    mFirstElementMonthlyKey = currentMonthlyKey;
                    mHeaderQuantityTextView.setText(String.format("%.02f", mTransactionsPerMonth.get(mFirstElementMonthlyKey))
                            + " " + getResources().getString(R.string.currency_aed));
                }
            }
        });
    }

    private void showTransactionsList() {
        mTransactionsListAdapter = new TransactionsListAdapter(mContext, mTransactionsList,
                mPresenter.getCompaniesMap(), mTransactionsPerMonth);
        mTransactionsRecyclerView.setAdapter(mTransactionsListAdapter);

        // Disable the no sms view
        mNoSmsTextView.setVisibility(View.GONE);
    }

    private void updateTransactionsPerMonth(ITransactions transaction) {
        // Intialize expense per month if needed
        if (mTransactionsPerMonth == null) {
            mTransactionsPerMonth = new HashMap<>();
        }

        long key = HeaderUtility.getHeaderMonthlyKeyByTransaction(transaction);

        if (!mTransactionsPerMonth.containsKey(key)) {
            mTransactionsPerMonth.put(key, 0.00f);
            transaction.setFirstTransactionOfTheMonth(true);
        } else {
            float monthExpense = mTransactionsPerMonth.get(key);
            monthExpense += transaction.getQuantity();
            mTransactionsPerMonth.put(key, monthExpense);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem showMonthlyGraphMenuItem = menu.add(Menu.NONE, MENU_ITEM_SHOW_MONTHLY_GRAPH_ID, Menu
            .NONE, R.string.action_bar_show_monthly_graph)
            .setIcon(R.drawable.ic_action_show_monthly_graph);
        showMonthlyGraphMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ITEM_SHOW_MONTHLY_GRAPH_ID:
                showMonthlyGraphs();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showMonthlyGraphs() {
        // If the data is not ready, don't do anything
        if (mTransactionsPerMonth == null || mTransactionsPerMonth.isEmpty()) {
            Log.w(TAG, "Trying to check the monthly transactions when the data is not ready");
            return;
        }

        Intent startMonthlyExpensesActivityIntent = new Intent(mContext, MonthlyTransactionsActivity.class);
        startMonthlyExpensesActivityIntent.putExtra(MonthlyTransactionsActivity.INTENT_KEY_MONTHLY_TRANSACTIONS,
            mTransactionsPerMonth);
        startActivity(startMonthlyExpensesActivityIntent);
        return;
    }

    // TODO: Use Dagger instead
    @Override
    public BasePresenter getPresenter() {
        return new TransactionsListPresenter(mContext);
    }
}