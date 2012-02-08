/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.sergioo.NetworkBusLaneAdder.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.xml.sax.SAXException;

import playground.sergioo.NetworkBusLaneAdder.kernel.AddressLocator;
import playground.sergioo.NetworkBusLaneAdder.kernel.BadAddressException;
import playground.sergioo.Visualizer2D.LayersWindow;


public class BusLaneAdderWindow extends LayersWindow implements ActionListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	//Enumerations
	private enum PanelIds implements LayersWindow.PanelIds {
		ONE;
	}
	public enum Options implements LayersWindow.Options {
		SELECT_NODES("<html>N<br/>O<br/>D<br/>E<br/>S</html>"),
		ZOOM("<html>Z<br/>O<br/>O<br/>M</html>");
		private String caption;
		private Options(String caption) {
			this.caption = caption;
		}
		@Override
		public String getCaption() {
			return caption;
		}
	}
	public enum Tool {
		FIND("Find address",0,0,2,1,"findAddress"),
		SELECT("Select path",2,0,2,1,"select"),
		ADD("Add new Links",4,0,2,1,"add"),
		SAVE("Save network",6,0,2,1,"save");
		String caption;
		int gx;int gy;
		int sx;int sy;
		String function;
		private Tool(String caption, int gx, int gy, int sx, int sy, String function) {
			this.caption = caption;
			this.gx = gx;
			this.gy = gy;
			this.sx = sx;
			this.sy = sy;
			this.function = function;
		}
	}
	public enum Labels implements LayersWindow.Labels {
		NODES("Nodes");
		private String text;
		private Labels(String text) {
			this.text = text;
		}
		@Override
		public String getText() {
			return text;
		}
	}
	
	//Attributes
	private JButton readyButton;
	private Network network;
	private String finalNetworkFile;
	CoordinateTransformation coordinateTransformation;
	
	//Methods
	public BusLaneAdderWindow(String title, Network network, File imageFile, Coord upLeft, Coord downRight, String finalNetworkFile, CoordinateTransformation coordinateTransformation) throws IOException {
		setTitle(title);
		this.finalNetworkFile = finalNetworkFile;
		this.network = network;
		this.coordinateTransformation = coordinateTransformation;
		NetworkTwoNodesPainter networkPainter = new NetworkTwoNodesPainter(network, Color.BLACK);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		this.setLocation(0,0);
		this.setLayout(new BorderLayout()); 
		layersPanels.put(PanelIds.ONE, new BusLaneAdderPanel(this, networkPainter, imageFile, upLeft, downRight));
		this.add(layersPanels.get(PanelIds.ONE), BorderLayout.CENTER);
		option = Options.ZOOM;
		JPanel toolsPanel = new JPanel();
		toolsPanel.setLayout(new GridBagLayout());
		for(Tool tool:Tool.values()) {
			JButton toolButton = new JButton(tool.caption);
			toolButton.setActionCommand(tool.name());
			toolButton.addActionListener(this);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = tool.gx;
			gbc.gridy = tool.gy;
			gbc.gridwidth = tool.sx;
			gbc.gridheight = tool.sy;
			toolsPanel.add(toolButton,gbc);
		}
		this.add(toolsPanel, BorderLayout.NORTH);
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new GridLayout(Options.values().length,1));
		for(Options option:Options.values()) {
			JButton optionButton = new JButton(option.getCaption());
			optionButton.setActionCommand(option.getCaption());
			optionButton.addActionListener(this);
			buttonsPanel.add(optionButton);
		}
		this.add(buttonsPanel, BorderLayout.EAST);
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BorderLayout());
		readyButton = new JButton("Ready to exit");
		readyButton.addActionListener(this);
		readyButton.setActionCommand(READY_TO_EXIT);
		infoPanel.add(readyButton, BorderLayout.WEST);
		JPanel labelsPanel = new JPanel();
		labelsPanel.setLayout(new GridLayout(1,Labels.values().length));
		labelsPanel.setBorder(new TitledBorder("Information"));
		labels = new JTextField[Labels.values().length];
		for(int i=0; i<Labels.values().length; i++) {
			labels[i]=new JTextField("");
			labels[i].setEditable(false);
			labels[i].setBackground(null);
			labels[i].setBorder(null);
			labelsPanel.add(labels[i]);
		}
		infoPanel.add(labelsPanel, BorderLayout.CENTER);
		JPanel coordsPanel = new JPanel();
		coordsPanel.setLayout(new GridLayout(1,2));
		coordsPanel.setBorder(new TitledBorder("Coordinates"));
		coordsPanel.add(lblCoords[0]);
		coordsPanel.add(lblCoords[1]);
		infoPanel.add(coordsPanel, BorderLayout.EAST);
		this.add(infoPanel, BorderLayout.SOUTH);
		pack();
	}
	public void refreshLabel(playground.sergioo.Visualizer2D.LayersWindow.Labels label) {
		labels[label.ordinal()].setText(((BusLaneAdderPanel)layersPanels.get(PanelIds.ONE)).getLabelText(label));
	}
	public Network getNetwork() {
		return network;
	}
	public void findAddress() {
		AddressLocator ad1 = new AddressLocator(JOptionPane.showInputDialog("Insert the desired address"));
		try {
			ad1.locate();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (BadAddressException e) {
			e.printStackTrace();
		}
		if(ad1.getNumResults()>1)
			JOptionPane.showMessageDialog(this, "Many results: "+ad1.getNumResults()+".");
		try {
			JOptionPane.showMessageDialog(this, ad1.getLocation().toString());
			((BusLaneAdderPanel)layersPanels.get(PanelIds.ONE)).centerCamera(coordinateTransformation.transform(ad1.getLocation()));
		} catch (HeadlessException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void select() {
		((BusLaneAdderPanel)layersPanels.get(PanelIds.ONE)).selectLinks();
	}
	public void save() {
		new NetworkWriter(network).write(finalNetworkFile);
	}
	public void exitSave() {
		new NetworkWriter(network).write(finalNetworkFile+"l");
	}
	public void add() throws Exception {
		List<Link> links = ((BusLaneAdderPanel)layersPanels.get(PanelIds.ONE)).getLinks();
		Node prevNode = links.get(0).getFromNode();
		for(Link link:links)
			if(!link.getAllowedModes().contains("bus")) {
				exitSave();
				throw new Exception("Wrong path, network saved");
			}
		for(int i=0; i<links.size(); i++) {
			Link link = links.get(i);
			Node node = null;
			if(i==links.size()-1)
				node = link.getToNode(); 
			else {
				node = network.getFactory().createNode(new IdImpl("n"+link.getToNode().getId().toString()), link.getToNode().getCoord());
				network.addNode(node);
			}
			LinkImpl newLink = (LinkImpl) network.getFactory().createLink(new IdImpl("c"+link.getId().toString()), prevNode, node);
			newLink.getAllowedModes().clear();
			newLink.getAllowedModes().add("car");
			newLink.setCapacity(link.getCapacity());
			newLink.setFreespeed(link.getFreespeed());
			newLink.setLength(link.getLength());
			newLink.setNumberOfLanes(link.getNumberOfLanes()-1);
			newLink.setOrigId(((LinkImpl)link).getOrigId());
			newLink.setType(((LinkImpl)link).getType());
			network.addLink(newLink);
			link.getAllowedModes().remove("car");
			link.setCapacity(900);
			prevNode=node;
		}
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		for(Options option:Options.values())
			if(e.getActionCommand().equals(option.getCaption()))
				this.option = option;
		for(Tool tool:Tool.values())
			if(e.getActionCommand().equals(tool.name())) {
				try {
					Method m = BusLaneAdderWindow.class.getMethod(tool.function, new Class[] {});
					m.invoke(this, new Object[]{});
				} catch (SecurityException e1) {
					e1.printStackTrace();
				} catch (NoSuchMethodException e1) {
					e1.printStackTrace();
				} catch (IllegalArgumentException e1) {
					e1.printStackTrace();
				} catch (IllegalAccessException e1) {
					e1.printStackTrace();
				} catch (InvocationTargetException e1) {
					e1.printStackTrace();
				}
				setVisible(true);
				repaint();
			}
		if(e.getActionCommand().equals(READY_TO_EXIT)) {
			setVisible(false);
			readyToExit = true;
		}
	}
	
	//Main
	public static final void main(String[] args) throws NumberFormatException, IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(args[0]);
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
		new BusLaneAdderWindow("Bus lanes adder", scenario.getNetwork(), new File(args[1]), coordinateTransformation.transform(new CoordImpl(Double.parseDouble(args[2]), Double.parseDouble(args[3]))), coordinateTransformation.transform(new CoordImpl(Double.parseDouble(args[4]), Double.parseDouble(args[5]))),args[6], coordinateTransformation).setVisible(true);
	}

}
