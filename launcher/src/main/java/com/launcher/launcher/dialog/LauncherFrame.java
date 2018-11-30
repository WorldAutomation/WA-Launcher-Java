package com.launcher.launcher.dialog;

import com.launcher.concurrency.ObservableFuture;
import com.launcher.launcher.FancyBackgroundPanel;
import com.launcher.launcher.Instance;
import com.launcher.launcher.InstanceList;
import com.launcher.launcher.FancyLauncher;
import com.launcher.launcher.launch.LaunchListener;
import com.launcher.launcher.launch.LaunchOptions;
import com.launcher.launcher.launch.LaunchOptions.UpdatePolicy;
import com.launcher.launcher.swing.*;
import com.launcher.launcher.util.SharedLocale;
import com.launcher.launcher.util.SwingExecutor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.ref.WeakReference;

import static com.launcher.launcher.util.SharedLocale.tr;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * The main launcher frame.
 */
@Log
public class LauncherFrame extends JFrame {

    private final FancyLauncher launcher;

    @Getter
    private final InstanceTable instancesTable = new InstanceTable();
    private final InstanceTableModel instancesModel;
    @Getter
    private final JScrollPane instanceScroll = new JScrollPane(instancesTable);
    private WebpagePanel webView;
    private JSplitPane splitPane;
    private final JButton launchButton = new JButton("<html><img src=https://www.worldautomation.net/images/launcher-launch.png>");
    private final JButton refreshButton = new JButton("<html><img src=https://www.worldautomation.net/images/launcher-refresh.png>");
    private final JButton optionsButton = new JButton("<html><img src=https://www.worldautomation.net/images/launcher-options.png>");
    private final JButton specsUpdateButton = new JButton("<html><img src=https://www.worldautomation.net/images/launcher-specs.png>");
    //private final JButton websiteButton = new JButton("<html><img src=https://www.worldautomation.net/images/launcher-web.png>");
    private final JCheckBox updateCheck = new JCheckBox(SharedLocale.tr("launcher.downloadUpdates"));
    private boolean isUpdateable = false;

    /**
     * Create a new frame.
     *
     * @param launcher the launcher
     */
    public LauncherFrame(@NonNull FancyLauncher launcher) {
        super(tr("launcher.title", launcher.getVersion()));

        this.launcher = launcher;
        instancesModel = new InstanceTableModel(launcher.getInstances());

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(400, 400));
        //setResizable(true);
        initComponents();
        pack();
        setLocationRelativeTo(null);

        SwingHelper.setFrameIcon(this, FancyLauncher.class, "icon.png");
        
        setSize(800, 530);
        setLocationRelativeTo(null);

        SwingHelper.removeOpaqueness(getInstancesTable());
        SwingHelper.removeOpaqueness(getInstanceScroll());
        getInstanceScroll().setBorder(BorderFactory.createEmptyBorder());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                loadInstances();
            }
        });
    }

    private void initComponents() {
        JPanel container = createContainerPanel();
        container.setLayout(new MigLayout("fill, insets dialog", "[][]push[][]", "[grow][]"));
        webView = createNewsPanel();
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, webView, instanceScroll);
		splitPane.setSize(800, 530);
		isUpdateable = launcher.getUpdateManager().getPendingUpdate();
        if (isUpdateable) {
            specsUpdateButton.setText("<html><img src=https://www.worldautomation.net/images/launcher-update.png>");
        } else {
            specsUpdateButton.setText("<html><img src=https://www.worldautomation.net/images/launcher-specs.png>");
        }
        specsUpdateButton.setVisible(true);
        launcher.getUpdateManager().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("pendingUpdate")) {
                    isUpdateable = (boolean) evt.getNewValue();
                    if (isUpdateable) {
                        specsUpdateButton.setText("<html><img src=https://www.worldautomation.net/images/launcher-update.png>");
                    } else {
                        specsUpdateButton.setText("<html><img src=https://www.worldautomation.net/images/launcher-specs.png>");
                    }

                }
            }
        });

        updateCheck.setSelected(true);
        instancesTable.setModel(instancesModel);
		//instancesTable.setFont(new Font("Courier", Font.PLAIN, 15));
		instancesTable.setRowHeight(20);
        //launchButton.setFont(new Font("Courier", Font.PLAIN, 15));
        splitPane.setDividerLocation(300);
        splitPane.setDividerSize(0);
        splitPane.setOpaque(false);
        container.add(splitPane, "grow, wrap, span 5, gapbottom unrel, w null:800, h null:550");
       //container.add(webView);
	   //container.add(instanceScroll);
	   //SwingHelper.flattenJSplitPane(splitPane);
	
        container.add(refreshButton);
        container.add(updateCheck);

	JButton discordButton = new JButton("<html><img src=https://www.worldautomation.net/images/launcher-discord.png>");
	container.add(discordButton);
	discordButton.addActionListener(ActionListeners.openURL(this, "https://discord.gg/Dvjvtee"));
	
	JButton webButton = new JButton("<html><img src=https://www.worldautomation.net/images/launcher-web.png>");
	container.add(webButton);
	webButton.addActionListener(ActionListeners.openURL(this, "https://www.worldautomation.net"));

	JButton logButton = new JButton("<html><img src=https://www.worldautomation.net/images/launcher-log.png>");
	container.add(logButton);
        
        container.add(specsUpdateButton);
        container.add(optionsButton);
        container.add(launchButton);

        add(container, BorderLayout.CENTER);

        instancesModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (instancesTable.getRowCount() > 0) {
                    instancesTable.setRowSelectionInterval(0, 0);
                }
            }
        });

        instancesTable.addMouseListener(new DoubleClickToButtonAdapter(launchButton));

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadInstances();
                launcher.getUpdateManager().checkForUpdate();
                webView.browse(launcher.getNewsURL(), false);
            }
        });

        specsUpdateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isUpdateable) {
                    launcher.getUpdateManager().performUpdate(LauncherFrame.this);
                } else {
                    showSpecs();
                }
            }
        });

        optionsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showOptions();
            }
        });

        launchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                launch();
            }
        });

        instancesTable.addMouseListener(new PopupMouseAdapter() {
            @Override
            protected void showPopup(MouseEvent e) {
                int index = instancesTable.rowAtPoint(e.getPoint());
                Instance selected = null;
                if (index >= 0) {
                    instancesTable.setRowSelectionInterval(index, index);
                    selected = launcher.getInstances().get(index);
                }
                popupInstanceMenu(e.getComponent(), e.getX(), e.getY(), selected);
            }
        });
		
		logButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ConsoleFrame.showMessages();
            }
        });
        
 		logButton.addMouseListener(new PopupMouseAdapter() {
            @Override
            protected void showPopup(MouseEvent e) {
                int index = instancesTable.rowAtPoint(e.getPoint());
                Instance selected = null;
                if (index >= 0) {
                    instancesTable.setRowSelectionInterval(index, index);
                    selected = launcher.getInstances().get(index);
                }
                popupInstanceMenu(e.getComponent(), e.getX(), e.getY(), selected);
            }
        });
		webButton.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent evt) {
				webButton.setText("WEB");
				webButton.setBackground(Color.GREEN);
				webButton.setPreferredSize(new Dimension(50, 39));
			}

			public void mouseExited(java.awt.event.MouseEvent evt) {
				webButton.setBackground(UIManager.getColor("control"));
				webButton.setText("<html><img src=https://www.worldautomation.net/images/launcher-web.png>");
				webButton.setPreferredSize(new Dimension(50, 30));
			}
		});
		logButton.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent evt) {
				logButton.setText("LOG");
				logButton.setBackground(Color.GREEN);
				logButton.setPreferredSize(new Dimension(50, 39));
			}

			public void mouseExited(java.awt.event.MouseEvent evt) {
				logButton.setBackground(UIManager.getColor("control"));
				logButton.setText("<html><img src=https://www.worldautomation.net/images/launcher-log.png>");
				logButton.setPreferredSize(new Dimension(50, 30));
			}
		});
		launchButton.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent evt) {
				launchButton.setText("PLAY");
				launchButton.setBackground(Color.GREEN);
				launchButton.setPreferredSize(new Dimension(50, 39));
			}

			public void mouseExited(java.awt.event.MouseEvent evt) {
				launchButton.setBackground(UIManager.getColor("control"));
				launchButton.setText("<html><img src=https://www.worldautomation.net/images/launcher-launch.png>");
				launchButton.setPreferredSize(new Dimension(50, 30));
			}
		});
		optionsButton.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent evt) {
				optionsButton.setText("OPTIONS");
				optionsButton.setBackground(Color.GREEN);
				optionsButton.setPreferredSize(new Dimension(50, 39));
			}

			public void mouseExited(java.awt.event.MouseEvent evt) {
				optionsButton.setBackground(UIManager.getColor("control"));
				optionsButton.setText("<html><img src=https://www.worldautomation.net/images/launcher-options.png>");
				optionsButton.setPreferredSize(new Dimension(50, 30));
			}
		});
		specsUpdateButton.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent evt) {
				specsUpdateButton.setText(isUpdateable ? "UPDATE" : "SPECS");
				specsUpdateButton.setBackground(Color.GREEN);
				specsUpdateButton.setPreferredSize(new Dimension(50, 39));
			}

			public void mouseExited(java.awt.event.MouseEvent evt) {
				specsUpdateButton.setBackground(UIManager.getColor("control"));
                                if (isUpdateable) {
                                    specsUpdateButton.setText("<html><img src=https://www.worldautomation.net/images/launcher-update.png>");
                                } else {
                                    specsUpdateButton.setText("<html><img src=https://www.worldautomation.net/images/launcher-specs.png>");
                                }
				specsUpdateButton.setPreferredSize(new Dimension(50, 30));
			}
		});
		refreshButton.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent evt) {
				refreshButton.setText("UPDATE");
				refreshButton.setBackground(Color.GREEN);
				refreshButton.setPreferredSize(new Dimension(50, 39));
			}

			public void mouseExited(java.awt.event.MouseEvent evt) {
				refreshButton.setBackground(UIManager.getColor("control"));
				refreshButton.setText("<html><img src=https://www.worldautomation.net/images/launcher-refresh.png>");
				//refreshButton.setPreferredSize(new Dimension(50, 30));				
			}
		});	
		discordButton.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent evt) {
				discordButton.setText("CHAT");
				discordButton.setBackground(Color.GREEN);
				discordButton.setPreferredSize(new Dimension(50, 39));
			}

			public void mouseExited(java.awt.event.MouseEvent evt) {
				discordButton.setBackground(UIManager.getColor("control"));
				discordButton.setText("<html><img src=https://www.worldautomation.net/images/launcher-discord.png>");
				//discordButton.setPreferredSize(new Dimension(50, 30));
			}
		});
		
	}

    protected JPanel createContainerPanel() {
        return new FancyBackgroundPanel();
    }

    /**
     * Return the news panel.
     *
     * @return the news panel
     */
    protected WebpagePanel createNewsPanel() {
        WebpagePanel panel = WebpagePanel.forURL(launcher.getNewsURL(), false);
        panel.setBrowserBorder(BorderFactory.createEmptyBorder());
        return panel;
    }

    /**
     * Popup the menu for the instances.
     *
     * @param component the component
     * @param x mouse X
     * @param y mouse Y
     * @param selected the selected instance, possibly null
     */
    private void popupInstanceMenu(Component component, int x, int y, final Instance selected) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem;

        if (selected != null) {
            menuItem = new JMenuItem(!selected.isLocal() ? tr("instance.install") : tr("instance.launch"));
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    launch();
                }
            });
            popup.add(menuItem);

            if (selected.isLocal()) {
                popup.addSeparator();

                menuItem = new JMenuItem(SharedLocale.tr("instance.openFolder"));
                menuItem.addActionListener(ActionListeners.browseDir(
                        LauncherFrame.this, selected.getContentDir(), true));
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.openSaves"));
                menuItem.addActionListener(ActionListeners.browseDir(
                        LauncherFrame.this, new File(selected.getContentDir(), "saves"), true));
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.openResourcePacks"));
                menuItem.addActionListener(ActionListeners.browseDir(
                        LauncherFrame.this, new File(selected.getContentDir(), "resourcepacks"), true));
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.openScreenshots"));
                menuItem.addActionListener(ActionListeners.browseDir(
                        LauncherFrame.this, new File(selected.getContentDir(), "screenshots"), true));
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.copyAsPath"));
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        File dir = selected.getContentDir();
                        dir.mkdirs();
                        SwingHelper.setClipboard(dir.getAbsolutePath());
                    }
                });
                popup.add(menuItem);

                popup.addSeparator();

                if (!selected.isUpdatePending()) {
                    menuItem = new JMenuItem(SharedLocale.tr("instance.forceUpdate"));
                    menuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            selected.setUpdatePending(true);
                            launch();
                            instancesModel.update();
                        }
                    });
                    popup.add(menuItem);
                }

                menuItem = new JMenuItem(SharedLocale.tr("instance.hardForceUpdate"));
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        confirmHardUpdate(selected);
                    }
                });
                popup.add(menuItem);

                menuItem = new JMenuItem(SharedLocale.tr("instance.deleteFiles"));
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        confirmDelete(selected);
                    }
                });
                popup.add(menuItem);
            }

            popup.addSeparator();
        }

        menuItem = new JMenuItem(SharedLocale.tr("launcher.refreshList"));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadInstances();
            }
        });
        popup.add(menuItem);

        popup.show(component, x, y);

    }

    private void confirmDelete(Instance instance) {
        if (!SwingHelper.confirmDialog(this,
                tr("instance.confirmDelete", instance.getTitle()), SharedLocale.tr("confirmTitle"))) {
            return;
        }

        ObservableFuture<Instance> future = launcher.getInstanceTasks().delete(this, instance);

        // Update the list of instances after updating
        future.addListener(new Runnable() {
            @Override
            public void run() {
                loadInstances();
            }
        }, SwingExecutor.INSTANCE);
    }

    private void confirmHardUpdate(Instance instance) {
        if (!SwingHelper.confirmDialog(this, SharedLocale.tr("instance.confirmHardUpdate"), SharedLocale.tr("confirmTitle"))) {
            return;
        }

        ObservableFuture<Instance> future = launcher.getInstanceTasks().hardUpdate(this, instance);

        // Update the list of instances after updating
        future.addListener(new Runnable() {
            @Override
            public void run() {
                launch();
                instancesModel.update();
            }
        }, SwingExecutor.INSTANCE);
    }

    private void loadInstances() {
        ObservableFuture<InstanceList> future = launcher.getInstanceTasks().reloadInstances(this);

        future.addListener(new Runnable() {
            @Override
            public void run() {
                instancesModel.update();
                if (instancesTable.getRowCount() > 0) {
                    instancesTable.setRowSelectionInterval(0, 0);
                }
                requestFocus();
            }
        }, SwingExecutor.INSTANCE);

        ProgressDialog.showProgress(this, future, SharedLocale.tr("launcher.checkingTitle"), SharedLocale.tr("launcher.checkingStatus"));
        SwingHelper.addErrorDialogCallback(this, future);
    }

    private void showOptions() {
        ConfigurationDialog configDialog = new ConfigurationDialog(this, launcher);
        configDialog.setVisible(true);
    }

    private void showSpecs() {
        SpecsDialog specsDialog = new SpecsDialog(this);
        specsDialog.setVisible(true);
    }

    private void launch() { // NOTICE: This enforces 64-bit Java!!!
        String version = System.getProperty("sun.arch.data.model");
        if(!version.contains("64")) {
            SwingHelper.showErrorDialog(null, "Uh oh! You need 64-Bit Java 8 minimum!", "WorldAutomation.Net");
            try {
                Desktop.getDesktop().browse(new URI("https://java.com/en/download/"));
            } catch (IOException | URISyntaxException e) {
            }
            return;
        }
        
        boolean permitUpdate = updateCheck.isSelected();
        Instance instance = launcher.getInstances().get(instancesTable.getSelectedRow());

        LaunchOptions options = new LaunchOptions.Builder()
                .setInstance(instance)
                .setListener(new LaunchListenerImpl(this))
                .setUpdatePolicy(permitUpdate ? UpdatePolicy.UPDATE_IF_SESSION_ONLINE : UpdatePolicy.NO_UPDATE)
                .setWindow(this)
                .build();
        launcher.getLaunchSupervisor().launch(options);
    }

    private static class LaunchListenerImpl implements LaunchListener {
        private final WeakReference<LauncherFrame> frameRef;
        private final FancyLauncher launcher;

        private LaunchListenerImpl(LauncherFrame frame) {
            this.frameRef = new WeakReference<>(frame);
            this.launcher = frame.launcher;
        }

        @Override
        public void instancesUpdated() {
            LauncherFrame frame = frameRef.get();
            if (frame != null) {
                frame.instancesModel.update();
            }
        }

        @Override
        public void gameStarted() {
            LauncherFrame frame = frameRef.get();
            if (frame != null) {
                frame.dispose();
            }
        }

        @Override
        public void gameClosed() {
            launcher.showLauncherWindow();
        }
    }

}
