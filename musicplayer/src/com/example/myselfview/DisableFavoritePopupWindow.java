package com.example.myselfview;

import com.example.musicplayer.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

@SuppressLint({ "ClickableViewAccessibility", "InflateParams" })
public class DisableFavoritePopupWindow extends PopupWindow {


	private Button btn_cancel, btn_ok;
	private RadioGroup pop_rg_layout;
	private TextView dialog_tv;
	private View mMenuView;

	public DisableFavoritePopupWindow(Activity context,int resource,OnClickListener itemsOnClick,
			OnCheckedChangeListener RadioGroupOnClick) {
		super(context);
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mMenuView = inflater.inflate(resource, null);
		pop_rg_layout = (RadioGroup)mMenuView.findViewById(R.id.pop_rg_layout);
		dialog_tv = (TextView)mMenuView.findViewById(R.id.dialog_tv);
		btn_ok = (Button) mMenuView.findViewById(R.id.btn_ok);
		btn_cancel = (Button) mMenuView.findViewById(R.id.btn_cancel);
		//取消按钮
		btn_cancel.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//销毁弹出框
				dismiss();
			}
		});
		//设置按钮监听
		btn_ok.setOnClickListener(itemsOnClick);
		btn_cancel.setOnClickListener(itemsOnClick);
		pop_rg_layout.setOnCheckedChangeListener(RadioGroupOnClick);
		//设置SelectPicPopupWindow的View
		this.setContentView(mMenuView);
		//设置SelectPicPopupWindow弹出窗体的宽
		this.setWidth(LayoutParams.FILL_PARENT);
		//设置SelectPicPopupWindow弹出窗体的高
		this.setHeight(LayoutParams.WRAP_CONTENT);
		//设置SelectPicPopupWindow弹出窗体可点击
		this.setFocusable(true);
		//设置SelectPicPopupWindow弹出窗体动画效果
		//this.setAnimationStyle(R.style.AnimBottom);
		//实例化一个ColorDrawable颜色为半透明
		ColorDrawable dw = new ColorDrawable(0xb0000000);
		//设置SelectPicPopupWindow弹出窗体的背景
		this.setBackgroundDrawable(dw);
		//mMenuView添加OnTouchListener监听判断获取触屏位置如果在选择框外面则销毁弹出框
		mMenuView.setOnTouchListener(new OnTouchListener() {
					
			public boolean onTouch(View v, MotionEvent event) {
				
				int height = mMenuView.findViewById(R.id.pop_layout).getTop();
				int y=(int) event.getY();
				if(event.getAction()==MotionEvent.ACTION_UP){
					if(y<height){
						dismiss();
					}
				}				
				return true;
			}
		});
		
	}

	public TextView getView(){
		return dialog_tv;
	}
	
}
