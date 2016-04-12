/*
 *    Copyright 2014 athenahealth, Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License"); you
 *   may not use this file except in compliance with the License.  You
 *   may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *   implied.  See the License for the specific language governing
 *   permissions and limitations under the License.
 */
																		
package com.apiclient;

import com.apiclient.APIConnection;
import org.apache.commons.lang.time.StopWatch;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;
import java.text.SimpleDateFormat;

class Testing {

	public static void main(String[] args) throws Exception {
		////////////////////////////////////////////////////////////////////////////////////////////
		// Setup
		////////////////////////////////////////////////////////////////////////////////////////////
		String key = "CHANGEME";
		String secret = "CHANGEME";
		String version = "preview1";
		String practiceid = "195900";
		String departmentId = "21";
		String appointmentTypeId = "4";
		JSONObject appt = null;
		String new_patient_id = null;

		
		APIConnection api = new APIConnection(version, key, secret, practiceid);


		StopWatch stopWatch = new StopWatch();

		stopWatch.start();
		GetCustomFields(api);
			printPerformanceNumber("Custom Fields Query", stopWatch);

		appt = GetAppointment(api, departmentId, appointmentTypeId);
			printPerformanceNumber("Appointment Query", stopWatch);

		new_patient_id = createPatient(api, departmentId);
			printPerformanceNumber("New Patient Registration", stopWatch);

		bookAppointment(api, departmentId, appointmentTypeId, appt, new_patient_id);
			printPerformanceNumber("Appointment booking", stopWatch);

		checkinAppointment(api, appt);
			printPerformanceNumber("Patient Check-in", stopWatch);

		removeChartAlert(api, departmentId, new_patient_id);
			printPerformanceNumber("Alert Chart Removal", stopWatch);

		removePatientPhoto(api, new_patient_id);
			printPerformanceNumber("Patient photo removal", stopWatch);

		ErrorCondition(api);
			printPerformanceNumber("Error Condition", stopWatch);

		RefreshToken(api);
			printPerformanceNumber("Refresh Token", stopWatch);
	}

	private static void printPerformanceNumber(String operationName, StopWatch stopWatch) {
		System.out.printf("%s,%s\n",operationName,stopWatch.getTime());
		stopWatch.reset();
		stopWatch.start();
	}

	private static void RefreshToken(APIConnection api) throws Exception {
		////////////////////////////////////////////////////////////////////////////////////////////
		// Testing token refresh
		//
		// NOTE: this test takes an hour, so it's disabled by default. Change false to true to run.
		////////////////////////////////////////////////////////////////////////////////////////////
		if (false) {
			String old_token = api.getToken();
			outputMessage("Old token: " + old_token);

			JSONObject before_refresh = (JSONObject) api.GET("/departments");

			// Wait 3600 seconds = 1 hour for token to expire.
			try {
				Thread.sleep(3600 * 1000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			JSONObject after_refresh = (JSONObject) api.GET("/departments");

			outputMessage("New token: " + api.getToken());
		}
	}

	private static void outputMessage(String s) {
//		System.out.println(s);
	}

	private static void ErrorCondition(APIConnection api) throws Exception {
		////////////////////////////////////////////////////////////////////////////////////////////
		// Error conditions
		////////////////////////////////////////////////////////////////////////////////////////////
		JSONObject bad_path = (JSONObject) api.GET("/nothing/at/this/path");
		outputMessage("GET /nothing/at/this/path:");
		outputMessage(bad_path.toString());
		JSONObject missing_parameters = (JSONObject) api.GET("/appointments/open");
		outputMessage("Missing parameters:");
		outputMessage(missing_parameters.toString());
	}

	private static void removePatientPhoto(APIConnection api, String new_patient_id) throws Exception {
		////////////////////////////////////////////////////////////////////////////////////////////
		// DELETE without parameters
		////////////////////////////////////////////////////////////////////////////////////////////
		JSONObject photo = (JSONObject) api.DELETE("/patients/" + new_patient_id + "/photo");
		outputMessage("Removed photo:");
		outputMessage(photo.toString());
	}

	private static void removeChartAlert(APIConnection api, String departmentId, String new_patient_id) throws Exception {
		////////////////////////////////////////////////////////////////////////////////////////////
		// DELETE with parameters
		////////////////////////////////////////////////////////////////////////////////////////////
		Map<String, String> delete_params = new HashMap<String, String>();
		delete_params.put("departmentid", departmentId);
		JSONObject chart_alert = (JSONObject) api.DELETE("/patients/" + new_patient_id + "/chartalert", delete_params);
		outputMessage("Removed chart alert:");
		outputMessage(chart_alert.toString());
	}

	private static void checkinAppointment(APIConnection api, JSONObject appt) throws Exception {
		////////////////////////////////////////////////////////////////////////////////////////////
		// POST without parameters
		////////////////////////////////////////////////////////////////////////////////////////////
		JSONObject checked_in = (JSONObject) api.POST("/appointments/" + appt.getString("appointmentid") + "/checkin");
		outputMessage("Check-in:");
		outputMessage(checked_in.toString());
	}

	private static void bookAppointment(APIConnection api, String departmentId, String appointmentTypeId, JSONObject appt, String new_patient_id) throws Exception {
		////////////////////////////////////////////////////////////////////////////////////////////
		// PUT with parameters
		////////////////////////////////////////////////////////////////////////////////////////////
		Map<String, String> appointment_info = new HashMap<String, String>();
		appointment_info.put("appointmenttypeid", appointmentTypeId);
		appointment_info.put("departmentid", departmentId);
		appointment_info.put("patientid", new_patient_id);

		JSONArray booked = (JSONArray) api.PUT("/appointments/" + appt.getString("appointmentid"), appointment_info);
		outputMessage("Booked:");
		outputMessage(booked.toString());
	}

	private static String createPatient(APIConnection api, String departmentId) throws Exception {
		String new_patient_id;////////////////////////////////////////////////////////////////////////////////////////////
		// POST with parameters
		////////////////////////////////////////////////////////////////////////////////////////////
		Map<String, String> patient_info = new HashMap<String, String>();
		patient_info.put("lastname", "Foo");
		patient_info.put("firstname", "Jason");
		patient_info.put("address1", "123 Any Street");
		patient_info.put("city", "Cambridge");
		patient_info.put("countrycode3166", "US");
		patient_info.put("departmentid", departmentId);
		patient_info.put("dob", "6/18/1987");
		patient_info.put("language6392code", "declined");
		patient_info.put("maritalstatus", "S");
		patient_info.put("race", "declined");
		patient_info.put("sex", "M");
		patient_info.put("ssn", "*****1234");
		patient_info.put("zip", "02139");

		JSONArray new_patient = (JSONArray) api.POST("/patients", patient_info);
		new_patient_id = new_patient.getJSONObject(0).getString("patientid");
		outputMessage("New patient id:");
		outputMessage(new_patient_id);
		return new_patient_id;
	}

	private static JSONObject GetAppointment(APIConnection api, String departmentId, String appointmentTypeId) throws Exception {
		JSONObject appt;////////////////////////////////////////////////////////////////////////////////////////////
		// GET with parameters
		////////////////////////////////////////////////////////////////////////////////////////////
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		Calendar today = Calendar.getInstance();
		Calendar nextyear = Calendar.getInstance();
		nextyear.roll(Calendar.YEAR, 1);
		Map<String, String> search = new HashMap<String, String>();
		search.put("departmentid", departmentId);//21, 164, 162, 157, 150, 148, 145, 142, 102, 62, 11
		search.put("startdate", format.format(today.getTime()));
		search.put("enddate", format.format(nextyear.getTime()));
		search.put("appointmenttypeid", appointmentTypeId);
		search.put("limit", "1");

		JSONObject open_appts = (JSONObject) api.GET("/appointments/open", search);
		outputMessage(open_appts.toString());
		appt = open_appts.getJSONArray("appointments").getJSONObject(0);
		outputMessage(" Open appointment:");
		outputMessage(appt.toString());

		// add keys to make appt usable for scheduling
		appt.put("appointmenttime", appt.get("starttime"));
		appt.put("appointmentdate", appt.get("date"));
		return appt;
	}

	private static void GetCustomFields(APIConnection api) throws Exception {
		////////////////////////////////////////////////////////////////////////////////////////////
		// GET without parameters
		////////////////////////////////////////////////////////////////////////////////////////////
		JSONArray customfields = (JSONArray) api.GET("/customfields");
		outputMessage("Custom fields:");
		for (int i = 0; i < customfields.length(); i++) {
			outputMessage("\t" + customfields.getJSONObject(i).get("name"));
		}
	}
}
