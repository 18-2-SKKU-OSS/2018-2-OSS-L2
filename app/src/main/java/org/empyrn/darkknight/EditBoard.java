package org.empyrn.darkknight;



import com.nemesis.materialchess.R;
import org.empyrn.darkknight.gamelogic.ChessParseError;
import org.empyrn.darkknight.gamelogic.Move;
import org.empyrn.darkknight.gamelogic.Piece;
import org.empyrn.darkknight.gamelogic.Position;
import org.empyrn.darkknight.gamelogic.TextIO;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A setting mode that allows you to change specific chessman into different chessman.
 * 특정 체스말을 다른 말로 바꿀 수 있는 설정 모드
 */

public class EditBoard extends Activity {
	private ChessBoardEdit cb;
	private TextView status;
	private Button okButton;
	private Button cancelButton;
    int choice,theme;
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initUI();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(EditBoard.this);
        theme = sharedPreferences.getInt("Theme",R.style.AppThemeOrange);
        loadTheme();
        super.setTheme(theme);

		Intent i = getIntent();
		Position pos;
		try {
			pos = TextIO.readFEN(i.getAction());
			cb.setPosition(pos);
		} catch (ChessParseError e) {
		}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            choice = sharedPreferences.getInt("Choice",4);
            switch(choice)
            {
                case 0: getWindow().setStatusBarColor(getResources().getColor(R.color.darkred));break;
                case 1:	getWindow().setStatusBarColor(getResources().getColor(R.color.darkblue));break;
                case 2:	getWindow().setStatusBarColor(getResources().getColor(R.color.darkpurple));break;
                case 3:	getWindow().setStatusBarColor(getResources().getColor(R.color.darkgreen));break;
                case 4:	getWindow().setStatusBarColor(getResources().getColor(R.color.darkorange));break;
                case 5:	getWindow().setStatusBarColor(getResources().getColor(R.color.darkgrey));break;
            }
        }
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		ChessBoardEdit oldCB = cb;
		String statusStr = status.getText().toString();
        initUI();
        cb.cursorX = oldCB.cursorX;
        cb.cursorY = oldCB.cursorY;
        cb.cursorVisible = oldCB.cursorVisible;
        cb.setPosition(oldCB.pos);
        cb.setSelection(oldCB.selectedSquare);
        status.setText(statusStr);
	}

	private final void initUI() {
		setContentView(R.layout.editboard);
		cb = (ChessBoardEdit)findViewById(R.id.eb_chessboard);
        status = (TextView)findViewById(R.id.eb_status);
		okButton = (Button)findViewById(R.id.eb_ok);
		cancelButton = (Button)findViewById(R.id.eb_cancel);

		okButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				sendBackResult();
			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		status.setFocusable(false);
        cb.setFocusable(true);
        cb.requestFocus();
        cb.setClickable(true);
        cb.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
		        if (event.getAction() == MotionEvent.ACTION_UP) {
		            int sq = cb.eventToSquare(event);
		            Move m = cb.mousePressed(sq);
		            if (m != null) {
		                doMove(m);
		            }
		            return false;
		        }
		        return false;
			}
		});
        cb.setOnTrackballListener(new ChessBoard.OnTrackballListener() {
        	public void onTrackballEvent(MotionEvent event) {
        		Move m = cb.handleTrackballEvent(event);
        		if (m != null) {
        			doMove(m);
        		}
        	}
        });
        cb.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				showDialog(EDIT_DIALOG);
				return true;
			}
		});
	}
	
	private void doMove(Move m) {
		if (m.to < 0) {
			if ((m.from < 0) || (cb.pos.getPiece(m.from) == Piece.EMPTY)) {
				cb.setSelection(m.to);
				return;
			}
		}
		Position pos = new Position(cb.pos);
		int piece = Piece.EMPTY;
		if (m.from >= 0) {
			piece = pos.getPiece(m.from);
		} else {
			piece = -(m.from + 2);
		}
		if (m.to >= 0)
			pos.setPiece(m.to, piece);
		if (m.from >= 0)
			pos.setPiece(m.from, Piece.EMPTY);
		cb.setPosition(pos);
		cb.setSelection(-1);
		checkValid();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			sendBackResult();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private final void sendBackResult() {
		if (checkValid()) {
			setPosFields();
			String fen = TextIO.toFEN(cb.pos);
			setResult(RESULT_OK, (new Intent()).setAction(fen));
		} else {
			setResult(RESULT_CANCELED);
		}
		finish();
	}

	private final void setPosFields() {
		setEPFile(getEPFile()); // To handle sideToMove change
		TextIO.fixupEPSquare(cb.pos);
		TextIO.removeBogusCastleFlags(cb.pos);
	}
	
	private final int getEPFile() {
		int epSquare = cb.pos.getEpSquare();
		if (epSquare < 0) return 8;
		return Position.getX(epSquare);
	}
	
	private final void setEPFile(int epFile) {
		int epSquare = -1;
		if ((epFile >= 0) && (epFile < 8)) {
			int epRank = cb.pos.whiteMove ? 5 : 2;
			epSquare = Position.getSquare(epFile, epRank);
		}
		cb.pos.setEpSquare(epSquare);
	}

	/** Test if a position is valid. */
	/** 유효한 위치인지 테스트해본다. */
	private final boolean checkValid() {
		try {
			String fen = TextIO.toFEN(cb.pos);
			TextIO.readFEN(fen);
			status.setText("");
			return true;
		} catch (ChessParseError e) {
			status.setText(e.getMessage());
		}
		return false;
	}

	static final int EDIT_DIALOG = 0; 
	static final int SIDE_DIALOG = 1; 
	static final int CASTLE_DIALOG = 2; 
	static final int EP_DIALOG = 3;
	static final int MOVCNT_DIALOG = 4;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case EDIT_DIALOG: {
			final CharSequence[] items = {
					getString(R.string.side_to_move),
					getString(R.string.clear_board), getString(R.string.initial_position),
					getString(R.string.castling_flags), getString(R.string.en_passant_file),
					getString(R.string.move_counters),
					getString(R.string.copy_position), getString(R.string.paste_position)
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.edit_board);
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	switch (item) {
			    	case 0: // Edit side to move
                            // 이동할 곳 편집
			    		showDialog(SIDE_DIALOG);
			    		cb.setSelection(-1);
			    		checkValid();
			    		break;
			    	case 1: { // Clear board
                              // 체스판 초기화
			    		Position pos = new Position();
			    		cb.setPosition(pos);
			    		cb.setSelection(-1);
			    		checkValid();
			    		break;
			    	}
			    	case 2: { // Set initial position
                              // 초기 위치 설정
			    		try {
			    			Position pos = TextIO.readFEN(TextIO.startPosFEN);
			    			cb.setPosition(pos);
			    			cb.setSelection(-1);
			    			checkValid();
			    		} catch (ChessParseError e) {
			    		}
			    		break;
			    	}
			    	case 3: // Edit castling flags
                            // 캐슬링 플래그 편집
			    		removeDialog(CASTLE_DIALOG);
			    		showDialog(CASTLE_DIALOG);
			    		cb.setSelection(-1);
			    		checkValid();
			    		break;
			    	case 4: // Edit en passant file
                            // en passant file 편집
			    		removeDialog(EP_DIALOG);
			    		showDialog(EP_DIALOG);
			    		cb.setSelection(-1);
			    		checkValid();
			    		break;
			    	case 5: // Edit move counters
                            // 이동 수 편집
			    		removeDialog(MOVCNT_DIALOG);
			    		showDialog(MOVCNT_DIALOG);
			    		cb.setSelection(-1);
			    		checkValid();
			    		break;
			    	case 6: { // Copy position
                              // 위치 복사
						setPosFields();
			    		String fen = TextIO.toFEN(cb.pos) + "\n";
			    		ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
			    		clipboard.setText(fen);
			    		cb.setSelection(-1);
			    		break;
			    	}
			    	case 7: { // Paste position
                              // 위치 붙여넣기
			    		ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
			    		if (clipboard.hasText()) {
			    			String fen = clipboard.getText().toString();
			    			try {
			    				Position pos = TextIO.readFEN(fen);
			    				cb.setPosition(pos);
			    			} catch (ChessParseError e) {
			    				if (e.pos != null)
					    			cb.setPosition(e.pos);
								Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
			    			}
			    			cb.setSelection(-1);
			    			checkValid();
			    		}
			    		break;
			    	}
			    	}
			    }
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		case SIDE_DIALOG: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.select_side_to_move_first)
			       .setPositiveButton(R.string.white, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   cb.pos.setWhiteMove(true);
			        	   checkValid();
			        	   dialog.cancel();
			           }
			       })
			       .setNegativeButton(R.string.black, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   cb.pos.setWhiteMove(false);
			        	   checkValid();
			        	   dialog.cancel();
			           }
			       });
			AlertDialog alert = builder.create();
			return alert;
		}
		case CASTLE_DIALOG: {
			final CharSequence[] items = {
				getString(R.string.white_king_castle), getString(R.string.white_queen_castle),
				getString(R.string.black_king_castle), getString(R.string.black_queen_castle)
			};
			boolean[] checkedItems = {
					cb.pos.h1Castle(), cb.pos.a1Castle(),
					cb.pos.h8Castle(), cb.pos.a8Castle()
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.castling_flags);
			builder.setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					Position pos = new Position(cb.pos);
					boolean a1Castle = pos.a1Castle();
					boolean h1Castle = pos.h1Castle();
					boolean a8Castle = pos.a8Castle();
					boolean h8Castle = pos.h8Castle();
					switch (which) {
					case 0: h1Castle = isChecked; break;
					case 1: a1Castle = isChecked; break;
					case 2: h8Castle = isChecked; break;
					case 3: a8Castle = isChecked; break;
					}
					int castleMask = 0;
					if (a1Castle) castleMask |= 1 << Position.A1_CASTLE;
					if (h1Castle) castleMask |= 1 << Position.H1_CASTLE;
					if (a8Castle) castleMask |= 1 << Position.A8_CASTLE;
					if (h8Castle) castleMask |= 1 << Position.H8_CASTLE;
					pos.setCastleMask(castleMask);
					cb.setPosition(pos);
					checkValid();
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		case EP_DIALOG: {
			final CharSequence[] items = {
					"A", "B", "C", "D", "E", "F", "G", "H", getString(R.string.none)
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.select_en_passant_file);
			builder.setSingleChoiceItems(items, getEPFile(), new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	setEPFile(item);
			    }
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		case MOVCNT_DIALOG: {
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.edit_move_counters);
			dialog.setTitle(R.string.edit_move_counters);
			final EditText halfMoveClock = (EditText)dialog.findViewById(R.id.ed_cnt_halfmove);
			final EditText fullMoveCounter = (EditText)dialog.findViewById(R.id.ed_cnt_fullmove);
			Button ok = (Button)dialog.findViewById(R.id.ed_cnt_ok);
			Button cancel = (Button)dialog.findViewById(R.id.ed_cnt_cancel);
			halfMoveClock.setText(String.format("%d", cb.pos.halfMoveClock));
			fullMoveCounter.setText(String.format("%d", cb.pos.fullMoveCounter));
			final Runnable setCounters = new Runnable() {
				public void run() {
					try {
				        int halfClock = Integer.parseInt(halfMoveClock.getText().toString());
				        int fullCount = Integer.parseInt(fullMoveCounter.getText().toString());
				        cb.pos.halfMoveClock = halfClock;
				        cb.pos.fullMoveCounter = fullCount;
						dialog.cancel();
					} catch (NumberFormatException nfe) {
						Toast.makeText(getApplicationContext(), R.string.invalid_number_format, Toast.LENGTH_SHORT).show();
					}
				}
			};
			fullMoveCounter.setOnKeyListener(new OnKeyListener() {
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
						setCounters.run();
						return true;
					}
					return false;
				}
	        });
			ok.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setCounters.run();
				}
			});
			cancel.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					dialog.cancel();
				}
			});
			return dialog;
		}
		}
		return null;
	}

    protected void loadTheme()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(EditBoard.this);
        theme = sharedPreferences.getInt("Theme",R.style.AppThemeRed);
        super.setTheme(theme);


    }
}
