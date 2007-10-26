/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.core.helpers;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.equinox.p2.core.helpers.messages"; //$NON-NLS-1$

	static {
		// load message values from bundle file and assign to fields below
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	public static String Util_Invalid_Zip_File_Format;
	public static String Util_Error_Unzipping;

	public static String XMLParser_No_SAX_Parser;
	public static String XMLParser_Error_At_Line;
	public static String XMLParser_Error_At_Line_Column;
	public static String XMLParser_Error_At_Name_Line;
	public static String XMLParser_Error_At_Name_Line_Column;
	public static String XMLParser_Missing_Required_Attribute;
	public static String XMLParser_Unexpected_Attribute;
	public static String XMLParser_Illegal_Value_For_Attribute;
	public static String XMLParser_Unexpected_Element;
	public static String XMLParser_Duplicate_Element;
	public static String XMLParser_Unexpected_Character_Data;
	public static String XMLParser_Element_Not_Allowed;

}
