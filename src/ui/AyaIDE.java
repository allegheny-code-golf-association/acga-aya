package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import aya.Aya;
import aya.AyaPrefs;
import aya.InteractiveAya;


@SuppressWarnings("serial")
public class AyaIDE extends JFrame
{	
	protected static String HELP_KEY_BINDINGS = ""
			+ "Quick Search: ctrl-q\n"
			+ "Interpreter: ctrl-i\n"
			+ "Editor: ctrl-e\n"
			+ "Run Editor: ctrl-r\n";
	protected static String HELP_ABOUT = "Aya\n"
			+ "Version: " + Aya.VERSION_NAME + "\n"
			+ "Source: github.com/nick-paul/aya-lang\n"
			+ "Wiki: github.com/nick-paul/aya-lang/wiki";

	
	
	private Aya _aya;
	
	//Layout
	private MyConsole _interpreter;
	private JMenu _menu;
	private JMenuBar _menuBar;
	
	private AyaIDE thiside;
    

	public AyaIDE(Aya aya) {
		super("Aya");
		
		this.thiside = this;

				
		this._aya = aya;
		this._interpreter = new MyConsole();
		this._menu = new JMenu();
		this._menuBar = new JMenuBar();
		
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				
		//Keyboard Listener
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
		  .addKeyEventDispatcher(new KeyEventDispatcher() {
			    public boolean dispatchKeyEvent(KeyEvent e) {
			    //System.out.println(e.getID() + " - " + e.getKeyCode());
			    	  
		    	  //On key up: 402, On key down: 401
		    	  if(e.getID() == 401) {
		    		  switch(e.getKeyCode()) {
		    		  case KeyEvent.VK_ENTER:
		    			  if(!_interpreter.getInputLine().getText().equals("") && _interpreter.getInputLine().inFocus()) {
		    				 // _aya.println(AyaPrefs.getPrompt() + " " + _interpreter.getInputLine().getText());
						  } 
		    			  break;
		    		  case KeyEvent.VK_I:
		    			  if(e.isControlDown()) {
		    				  _interpreter.getInputLine().grabFocus();
		    			  }
		    			  break;
		    		  case KeyEvent.VK_Q:
		    			  if(e.isControlDown()) {
		    				  if(QuickSearch.isFrameActive()) {
			    				  QuickSearch.updateHelpTextInFrame(Aya.getQuickSearchData());
		    					  QuickSearch.frameFocus();
		    				  } else {
		    					  QuickSearch.newQSFrame(Aya.getQuickSearchData());
		    				  }
		    			  }
		    			  break;
		    		  case KeyEvent.VK_E:
		    			  if(e.isControlDown()) {
		    				  if(EditorWindow.isFrameActive()) {
		    					  EditorWindow.frameFocus();
		    				  } else {
		    					  EditorWindow.newEditorFrame(thiside);
		    				  }
		    			  }
		    			  break;
		    		  case KeyEvent.VK_R:
		    			  if(e.isControlDown()) {
		    				  if (EditorWindow.activeEditor == null) {
		    						JOptionPane.showMessageDialog(_interpreter, "No editor window open", "ERROR", JOptionPane.ERROR_MESSAGE);
		    				  }
		    				  EditorWindow.activeEditor.run();
		    			  }
		    			  break;
		    		  }
		    	  }
		    	  return false;
			}
		});
		
		//Confirm Close
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) { 
				exit();
		    }
		});
		
		//Menu Bar
		_menuBar.setPreferredSize(new Dimension(100, 20));
		
		//File
		_menu = new JMenu("File");
		_menu.setMnemonic(KeyEvent.VK_A);
		_menu.getAccessibleContext().setAccessibleDescription("");
		//Load
		JMenuItem mi =new JMenuItem(new Action() {
			public void actionPerformed(ActionEvent e) {
				String path = requestFilePathUI();
				if (path != null) {
					path = path.replace("\\", "\\\\");
					Aya.getInstance().queueInput("\"" + path + "\"G~");
					
				}
			}
			public void addPropertyChangeListener(PropertyChangeListener l) {}
			public Object getValue(String k) {return null;}
			public boolean isEnabled() {return true;}
			public void putValue(String k, Object v) {}
			public void removePropertyChangeListener(PropertyChangeListener l) {}
			public void setEnabled(boolean b) {}
		});
		mi.setText("Load");
		_menu.add(mi);


		
		_menuBar.add(_menu);
			
		//Tools
		_menu = new JMenu("Tools");
		_menu.setMnemonic(KeyEvent.VK_A);
		_menu.getAccessibleContext().setAccessibleDescription("");
		//Insert Filename
		mi =new JMenuItem(new Action() {
			public void actionPerformed(ActionEvent e) {
				insertFilenameAtCarat();
			}
			public void addPropertyChangeListener(PropertyChangeListener l) {}
			public Object getValue(String k) {return null;}
			public boolean isEnabled() {return true;}
			public void putValue(String k, Object v) {}
			public void removePropertyChangeListener(PropertyChangeListener l) {}
			public void setEnabled(boolean b) {}
		});
		mi.setText("Insert Filename..");
		_menu.add(mi);
		//Clear Console
		mi =new JMenuItem(new Action() {
			public void actionPerformed(ActionEvent e) {
				_interpreter.clear();
			}
			public void addPropertyChangeListener(PropertyChangeListener l) {}
			public Object getValue(String k) {return null;}
			public boolean isEnabled() {return true;}
			public void putValue(String k, Object v) {}
			public void removePropertyChangeListener(PropertyChangeListener l) {}
			public void setEnabled(boolean b) {}
		});
		mi.setText("Clear Console");
		_menu.add(mi);
		mi =new JMenuItem(new Action() {
			public void actionPerformed(ActionEvent e) {
				if(EditorWindow.isFrameActive()) {
					  EditorWindow.frameFocus();
				  } else {
					  EditorWindow.newEditorFrame(thiside);
				  }
			}
			public void addPropertyChangeListener(PropertyChangeListener l) {}
			public Object getValue(String k) {return null;}
			public boolean isEnabled() {return true;}
			public void putValue(String k, Object v) {}
			public void removePropertyChangeListener(PropertyChangeListener l) {}
			public void setEnabled(boolean b) {}
		});
		mi.setText("Open Editor   ctrl+E");
		_menu.add(mi);		
		
		_menuBar.add(_menu);
		
		
		//Help
		//Quick Search
		_menu = new JMenu("Help");
		_menu.setMnemonic(KeyEvent.VK_A);
		_menu.getAccessibleContext().setAccessibleDescription("");
		mi = new JMenuItem(new Action() {
			public void actionPerformed(ActionEvent e) {
				if(QuickSearch.isFrameActive()) {
					QuickSearch.frameFocus();
				} else {
					QuickSearch.newQSFrame(Aya.getQuickSearchData());
				}
			}
			public void addPropertyChangeListener(PropertyChangeListener l) {}
			public Object getValue(String k) {return null;}
			public boolean isEnabled() {return true;}
			public void putValue(String k, Object v) {}
			public void removePropertyChangeListener(PropertyChangeListener l) {}
			public void setEnabled(boolean b) {}
		});
		mi.setText("Quick Search");
		_menu.add(mi);
		
		//Key Bindings
		mi = new JMenuItem(new Action() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(_interpreter, HELP_KEY_BINDINGS);
			}
			public void addPropertyChangeListener(PropertyChangeListener l) {}
			public Object getValue(String k) {return null;}
			public boolean isEnabled() {return true;}
			public void putValue(String k, Object v) {}
			public void removePropertyChangeListener(PropertyChangeListener l) {}
			public void setEnabled(boolean b) {}
		});
		mi.setText("Key Bindings");
		_menu.add(mi);
		mi = new JMenuItem(new Action() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(_interpreter, HELP_ABOUT);
			}
			public void addPropertyChangeListener(PropertyChangeListener l) {}
			public Object getValue(String k) {return null;}
			public boolean isEnabled() {return true;}
			public void putValue(String k, Object v) {}
			public void removePropertyChangeListener(PropertyChangeListener l) {}
			public void setEnabled(boolean b) {}
		});
		mi.setText("About");
		_menu.add(mi);
		_menuBar.add(_menu);
		
		JPanel all = new JPanel();
		all.setLayout(new BorderLayout());


		
		JPanel smallConsole = new JPanel();
		smallConsole.setLayout(new BorderLayout());
		smallConsole.add(_interpreter, BorderLayout.CENTER);
		all.add(smallConsole);
		
		add(all);
		
		setJMenuBar(_menuBar);
		pack();
		setVisible(true);
	
		
		_interpreter.getInputLine().grabFocus();
	}
	
	public static void exit() {
		if(EditorWindow.hasText()) {
			String ObjButtons[] = {"Yes","No"};
		    int PromptResult = JOptionPane.showOptionDialog(null, 
		        "Editor still has code. Are you sure you want to exit?", "Editor Has Code", 
		        JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, 
		        ObjButtons,ObjButtons[1]);
		    if(PromptResult==0) {
		      System.exit(0);          
		    }
		} else {
			System.exit(0);
		}
	}
	
	public void insertFilenameAtCarat() {
		File file = chooseFile();
		if(file != null) {
			String path = file.getPath();
			path = path.replace("\\", "\\\\");
            _interpreter.getInputLine().insertAtCaret("\"" + path + "\"");
		}
	}
	
	public String requestFilePathUI() {
		File file = chooseFile();
		if(file != null) {
			return file.getPath();
		}
		return null;
	}
	
	public static File chooseFile() {
		JFileChooser fc = new JFileChooser();
		
		//Set working directory
		File here = new File(".");
		fc.setCurrentDirectory(here);
		here.delete();
		
		int returnVal = fc.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        } else {
        	return null;
        }
	}
	
	public Aya getAya() {
		return this._aya;
	}
	
	public OutputStream getOutputStream() {
		return _interpreter.getOut();
	}
	
	public InputStream getInputStream() {
		return _interpreter.getIn();
	}
	
	public static void main(String[] args) {
		
		Aya aya = Aya.getInstance();
		boolean readstdin = aya.isInputAvaiable();
		if (args.length > 0) {
			// First arg is working directory
			AyaPrefs.setWorkingDir(args[0]);
		}
		if (args.length > 1 || readstdin) {
			// If reading from STDIN (piped input), don't use interactive mode
			if (readstdin) InteractiveAya.setInteractive(false);
			InteractiveAya.main(args);
		} else {
			// Use the GUI
			
			//Load and initialize the ide
			AyaIDE ide = new AyaIDE(aya);
			
			// Aya Prefs
			aya.setOut(ide.getOutputStream());
			aya.setErr(ide.getOutputStream());
			aya.setIn(ide.getInputStream());
			
			// InteractiveAya Prefs
			InteractiveAya iaya = new InteractiveAya(aya);
			iaya.setPromptText(false);
			iaya.setEcho(true);
			iaya.showBanner(false);
			iaya.run();
			
			//Grab focus
			ide._interpreter.getInputLine().grabFocus();
			
			try {
				iaya.join();
			} catch (InterruptedException e) {
				e.printStackTrace(aya.getErr());
			}
			
			System.exit(1);
		}
	
	}
}


