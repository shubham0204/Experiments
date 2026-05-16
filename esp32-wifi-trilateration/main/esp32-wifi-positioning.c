#include "esp_log.h"
#include "esp_wifi.h"
#include "nvs_flash.h"
#include <math.h>
#include <stdio.h>
#include <stdlib.h>

#include "trilateration.c"
#include "ap_list.c"

#define MAX_AP_INFO_COUNT 10
#define LOG_TAG "wifi-scan"

/** WiFi Access Point */
typedef struct {
    string_t ssid;  // service set identifier (SSId)
    int8_t rssi;    // received signal strength indicator (RSSI), measured 
                    // in dBm (decibel-milliwatts)
} ap_t;

double rssi_to_dist(int8_t rssi) {
    int tx_power = -30;
    double path_loss = 3.0;
    if (rssi >= tx_power) {
        return 1.0;
    }
    double exponent = (double)(tx_power - rssi) / (10.0 * path_loss);
    return pow(10.0, exponent) / 111320.0;
}

int compute(const ap_t* detected_aps, size_t detected_aps_len, pos_vec* estimated_loc) {
    // compute top-3 nearest APs based on RSSI
    ap_t top_3_nearest_aps[3];
    uint8_t element_marked[detected_aps_len];
    memset(element_marked, 0, sizeof(element_marked));
    for (int i = 0; i < 3; i++) {
        double max_rssi = -1e+10;
        int max_rssi_idx = -1;
        for (int j = 0; j < detected_aps_len; j++) {
            if ((detected_aps[j].rssi > max_rssi) &&
                (element_marked[j] != 1)) {
                max_rssi = detected_aps[j].rssi;
                max_rssi_idx = j;
            }
        }
        if (max_rssi_idx == -1) {
            return TRIL_NOT_ENOUGH_APS_DETECTED;
        }
        element_marked[max_rssi_idx] = 1;
        top_3_nearest_aps[i] = detected_aps[max_rssi_idx];
        printf("top detected %d: %s\n", i,
               detected_aps[max_rssi_idx].ssid.data);
    }

    // get the positions of the top-3 nearest APs 
    // looking up the ap_list
    ap_ssid_loc_map_record_t rec1;
    if (!get_ap_from_ssid(&top_3_nearest_aps[0].ssid, &rec1)) {
        return TRIL_AP_RECORD_NOT_FOUND;
    }
    ap_ssid_loc_map_record_t rec2;
    if (!get_ap_from_ssid(&top_3_nearest_aps[1].ssid, &rec2)) {
        return TRIL_AP_RECORD_NOT_FOUND;
    }
    ap_ssid_loc_map_record_t rec3;
    if (!get_ap_from_ssid(&top_3_nearest_aps[2].ssid, &rec3)) {
        return TRIL_AP_RECORD_NOT_FOUND;
    }

    pos_vec p1 = {.x = rec1.ap_loc_x,
                  .y = rec1.ap_loc_y,
                  .r = rssi_to_dist(top_3_nearest_aps[0].rssi)};
    pos_vec p2 = {.x = rec2.ap_loc_x,
                  .y = rec2.ap_loc_y,
                  .r = rssi_to_dist(top_3_nearest_aps[1].rssi)};
    pos_vec p3 = {.x = rec3.ap_loc_x,
                  .y = rec3.ap_loc_y,
                  .r = rssi_to_dist(top_3_nearest_aps[2].rssi)};

    *estimated_loc = trilaterate(p1, p2, p3);
    return 0;
}

void wifi_scan() {
    ESP_ERROR_CHECK(esp_netif_init()); // initialize underlying tcp/ip stack
    ESP_ERROR_CHECK(
        esp_event_loop_create_default()); // system loop to handle async events
    esp_netif_create_default_wifi_sta();  // create wifi station config,
                                          // attaches to default event loop

    wifi_init_config_t config =
        WIFI_INIT_CONFIG_DEFAULT(); // wifi config with defaults (configures
                                    // buffers, beacons etc.)
    ESP_ERROR_CHECK(esp_wifi_init(&config));

    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA));
    ESP_ERROR_CHECK(esp_wifi_start());

    wifi_ap_record_t access_points[MAX_AP_INFO_COUNT];

    while (true) {
        esp_wifi_scan_start(NULL, true);
        uint16_t access_point_count = 0;
        uint16_t max_ap_scan_count = MAX_AP_INFO_COUNT;
        ESP_ERROR_CHECK(esp_wifi_scan_get_ap_num(&access_point_count));
        ESP_ERROR_CHECK(
            esp_wifi_scan_get_ap_records(&max_ap_scan_count, access_points));
        ESP_LOGI(LOG_TAG, "ap count is %d", access_point_count);

        ap_t detected_aps[max_ap_scan_count];
        for (int i = 0; i < max_ap_scan_count; i++) {
            ESP_LOGI(LOG_TAG, "%s: %d", access_points[i].ssid,
                     access_points[i].rssi);
            ap_t ap;
            ap.ssid = str_from_bytearr(access_points[i].ssid);
            ap.rssi = access_points[i].rssi;
            detected_aps[i] = ap;
        }
        pos_vec estimated_loc;
        int ret = compute(detected_aps, max_ap_scan_count, &estimated_loc);
        if (ret == 0) {
            ESP_LOGI(LOG_TAG, "estimated x: %f, y: %f", estimated_loc.x,
                     estimated_loc.y);
        }
        vTaskDelay(pdMS_TO_TICKS(5000));
    }
}

void nvs_init() {
    // initializes non-volatile storage (NVS) for storing wifi credentials
    // the credentials are stored in NVS to ensure that the micro-controller
    // connects to the wifi access point (AP) on boot.
    // data in NVS is preserved across reboots.
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES ||
        ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);
}

void app_main(void) {
    nvs_init();
    wifi_scan();
}
