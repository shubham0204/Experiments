#include "strings.c"

static const int TRIL_NOT_ENOUGH_APS_DETECTED = 1;
static const int TRIL_AP_RECORD_NOT_FOUND = 2;

typedef struct {
    string_t ssid;
    double ap_loc_x;
    double ap_loc_y;
} ap_ssid_loc_map_record_t;

/*
Add lat/lng of the known access points
Trilateration will use these access point coordinates along with the estimated distance
to determine the location of the micro-controller.
Each access point is identified by its SSID
*/
static const ap_ssid_loc_map_record_t ap_loc_records[] = {
    {.ssid = {.data = "________________", .length = 16},
     .ap_loc_x = 18.5212345,
     .ap_loc_y = 73.851212},
    {.ssid = {.data = "_______________________", .length = 23},
     .ap_loc_x = 23.2153523,
     .ap_loc_y = 76.406346},
    {.ssid = {.data = "___________", .length = 11},
     .ap_loc_x = 15.8034534,
     .ap_loc_y = 79.10345345},
    {.ssid = {.data = "_______ ", .length = 7},
     .ap_loc_x = 15.8124525,
     .ap_loc_y = 79.12235235},
    {.ssid = {.data = "_____________", .length = 13},
     .ap_loc_x = 21.0523523,
     .ap_loc_y = 70.30234235}};

bool get_ap_from_ssid(const string_t* ssid,
                      ap_ssid_loc_map_record_t* retrieved_record) {
    size_t num_records = sizeof(ap_loc_records) / sizeof(ap_loc_records[0]);
    for (int i = 0; i < num_records; i++) {
        if (streq(&ap_loc_records[i].ssid, ssid)) {
            *retrieved_record = ap_loc_records[i];
            return true;
        }
    }
    return false;
}