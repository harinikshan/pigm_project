/* Copyright (c) 2014 Nordic Semiconductor. All Rights Reserved.
 *
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC
 * SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 *
 * Licensees are granted free, non-transferable use of the information. NO
 * WARRANTY of ANY KIND is provided. This heading must NOT be removed from
 * the file.
 *
 */
/** @example examples/ble_peripheral/ble_app_hrs/main.c
 *
 * @brief Heart Rate Service Sample Application main file.
 *
 * This file contains the source code for a sample application using the Heart Rate service
 * (and also Battery and Device Information services). This application uses the
 * @ref srvlib_conn_params module. 
 */

#include <stdint.h>
#include <string.h>
#include "nordic_common.h"
#include "nrf.h"
#include "app_error.h"
#include "ble.h"
#include "ble_hci.h"
#include "ble_srv_common.h"
#include "ble_advdata.h"
#include "ble_advertising.h"
#include "ble_bas.h"
#include "ble_hrs.h"
#include "ble_dis.h"
#include  "nrf_power.h"
#include "nrf_drv_adc.h"
#define BLE_DFU_APP_SUPPORT  1

#ifdef BLE_DFU_APP_SUPPORT                    
#include "ble_dfu.h"
#include "dfu_app_handler.h"
#endif // BLE_DFU_APP_SUPPORT
#include "ble_conn_params.h"
#include "boards.h"
#include "sensorsim.h"
#include "softdevice_handler.h"
#include "app_timer.h"
#include "device_manager.h"
#include "pstorage.h"
#include "app_trace.h"
#include "bsp.h"
#include "nrf_delay.h"
#include "bsp_btn_ble.h"


#include "nrf_drv_twi.h"
#include "app_util_platform.h"
#include "nrf_delay.h"

static void advertising_init(void);
extern uint32_t epoch_time;



extern uint32_t dyn_sen_rd_time_interval;
extern uint16_t adv_interval;
extern uint16_t sensor_wakeup_time;
extern uint16_t sensor_active_time;


uint8_t tx_state = 0;
uint8_t bat_status;

bool Device_Connection_State=false; 

bool Live_data_flag=false;
//extern bool sensor_en_flag;

volatile bool sensor_en_flag=false;
volatile bool sensor_dis_flag=false;
bool data_store_flag=false;
bool no_data_flag=false;
bool sensor_wakeup_time_update_flag=false;
//bool twi_flag=false;

uint16_t sensor_en_time=0; 
uint16_t sensor_dis_time=0;
extern volatile uint8_t device_name[];
uint8_t extern hr_led_int_config[2];

bool device_name_updated=false;
bool epoch_time_receive=false;
bool tx_complete=false;
bool data_tx_flag=false;
bool sensor_data_rx_flag=true;
bool over_write_flag=false;
extern bool timer_stop;
extern bool current_adv_rd_time_response;
bool data_tx_ack_flag=false;
bool pin_enable_flag=false;
bool adverising_stop_flag=false;

#define MAX30100_GET_HEART_RATE_VALUE(first_byte,second_byte,third_byte) \
    (((((int32_t)first_byte << 16) |((uint16_t)(second_byte<<8)|(third_byte))))/4336)
	
#define MAX30100_GET_SPO2_VALUE(first_byte,second_byte,third_byte) \
    (((((int32_t)first_byte << 16) |((uint16_t)(second_byte<<8)|(third_byte))))/9300)


	
#define MAX30100_GET_TEMPERATURE_VALUE(temp_hi,temp_lo)\
     (temp_hi+(temp_lo*0.0625))


#define PACKET_LEN                   17
#define NUMBER_OF_SAMPLES            15300
#define MAX30100_ADDRESS             0x57
#define LIS2DH12_ADDRESS             0x18     
#define BUFFER_SIZE                  12
#define ADC_BUFFER_SIZE              1                                /**< Size of buffer for ADC samples.  */
#define ADC_MAX_STEP_SIZE            938
#define MIN_ADC_STEP_SIZE            837

static uint8_t m_buffer[BUFFER_SIZE];
uint8_t m_samples[NUMBER_OF_SAMPLES]={0};
uint16_t  m_sample_write_idx=0;
uint16_t m_sample_read_idx=0;


static nrf_drv_twi_t m_twi = 	NRF_DRV_TWI_INSTANCE(0);








#define IS_SRVC_CHANGED_CHARACT_PRESENT  1                                          /**< Include or not the service_changed characteristic. if not enabled, the server's database cannot be changed for the lifetime of the device*/

#define CENTRAL_LINK_COUNT               0                                          /**< Number of central links used by the application. When changing this number remember to adjust the RAM settings*/
#define PERIPHERAL_LINK_COUNT            1                                          /**< Number of peripheral links used by the application. When changing this number remember to adjust the RAM settings*/

#define MANUFACTURER_NAME               "NordicSemiconductor"                      /**< Manufacturer. Will be passed to Device Information Service. */
#define APP_ADV_INTERVAL                 800                                       /**< The advertising interval (in units of 0.625 ms. This value corresponds to 25 ms). */
#define APP_ADV_TIMEOUT_IN_SECONDS       0                                        /**< The advertising timeout in units of seconds. */

#define APP_TIMER_PRESCALER              0                                          /**< Value of the RTC1 PRESCALER register. */
#define APP_TIMER_OP_QUEUE_SIZE          6                                          /**< Size of timer operation queues. */

#define BATTERY_LEVEL_MEAS_INTERVAL      APP_TIMER_TICKS(2000, APP_TIMER_PRESCALER) /**< Battery level measurement interval (ticks). */
#define MIN_BATTERY_LEVEL                81                                         /**< Minimum simulated battery level. */
#define MAX_BATTERY_LEVEL                100                                        /**< Maximum simulated 7battery level. */
#define BATTERY_LEVEL_INCREMENT          1                                          /**< Increment between each simulated battery level measurement. */

#define HEART_RATE_MEAS_INTERVAL         APP_TIMER_TICKS(1000, APP_TIMER_PRESCALER) /**< Heart rate measurement interval (ticks). */
#define MIN_HEART_RATE                   140                                        /**< Minimum heart rate as returned by the simulated measurement function. */
#define MAX_HEART_RATE                   300                                        /**< Maximum heart rate as returned by the simulated measurement function. */
#define HEART_RATE_INCREMENT             10                                         /**< Value by which the heart rate is incremented/decremented for each call to the simulated measurement function. */

#define RR_INTERVAL_INTERVAL             APP_TIMER_TICKS(300, APP_TIMER_PRESCALER)  /**< RR interval interval (ticks). */
#define MIN_RR_INTERVAL                  100                                        /**< Minimum RR interval as returned by the simulated measurement function. */
#define MAX_RR_INTERVAL                  500                                        /**< Maximum RR interval as returned by the simulated measurement function. */
#define RR_INTERVAL_INCREMENT            1                                          /**< Value by which the RR interval is incremented/decremented for each call to the simulated measurement function. */

#define SENSOR_CONTACT_DETECTED_INTERVAL APP_TIMER_TICKS(5000, APP_TIMER_PRESCALER) /**< Sensor Contact Detected toggle interval (ticks). */

#define MIN_CONN_INTERVAL                MSEC_TO_UNITS(100, UNIT_1_25_MS)            /**< Minimum acceptable connection interval (0.4 seconds). */
#define MAX_CONN_INTERVAL                MSEC_TO_UNITS(200, UNIT_1_25_MS)           /**< Maximum acceptable connection interval (0.65 second). */
#define SLAVE_LATENCY                    0                                          /**< Slave latency. */
#define CONN_SUP_TIMEOUT                 MSEC_TO_UNITS(4000, UNIT_10_MS)            /**< Connection supervisory timeout (4 seconds). */

#define FIRST_CONN_PARAMS_UPDATE_DELAY   APP_TIMER_TICKS(5000, APP_TIMER_PRESCALER) /**< Time from initiating event (connect or start of notification) to first time sd_ble_gap_conn_param_update is called (5 seconds). */
#define NEXT_CONN_PARAMS_UPDATE_DELAY    APP_TIMER_TICKS(30000, APP_TIMER_PRESCALER)/**< Time between each call to sd_ble_gap_conn_param_update after the first call (30 seconds). */
#define MAX_CONN_PARAMS_UPDATE_COUNT     3                                          /**< Number of attempts before giving up the connection parameter negotiation. */

#define SEC_PARAM_BOND                   1                                          /**< Perform bonding. */
#define SEC_PARAM_MITM                   0                                          /**< Man In The Middle protection not required. */
#define SEC_PARAM_LESC                   0                                          /**< LE Secure Connections not enabled. */
#define SEC_PARAM_KEYPRESS               0                                          /**< Keypress notifications not enabled. */
#define SEC_PARAM_IO_CAPABILITIES        BLE_GAP_IO_CAPS_NONE                       /**< No I/O capabilities. */
#define SEC_PARAM_OOB                    0                                          /**< Out Of Band data not available. */
#define SEC_PARAM_MIN_KEY_SIZE           7                                          /**< Minimum encryption key size. */
#define SEC_PARAM_MAX_KEY_SIZE           16                                         /**< Maximum encryption key size. */

#define DEAD_BEEF                        0xDEADBEEF                                 /**< Value used as error code on stack dump, can be used to identify stack location on stack unwind. */
#ifdef BLE_DFU_APP_SUPPORT
#define DFU_REV_MAJOR                    0x00                                       /** DFU Major revision number to be exposed. */
#define DFU_REV_MINOR                    0x01                                       /** DFU Minor revision number to be exposed. */
#define DFU_REVISION                     ((DFU_REV_MAJOR << 8) | DFU_REV_MINOR)     /** DFU Revision number to be exposed. Combined of major and minor versions. */
#define APP_SERVICE_HANDLE_START         0x000C                                     /**< Handle of first application specific service when when service changed characteristic is present. */
#define BLE_HANDLE_MAX                   0xFFFF                                     /**< Max handle value in BLE. */

STATIC_ASSERT(IS_SRVC_CHANGED_CHARACT_PRESENT);                                     /** When having DFU Service support in application the Service Changed Characteristic should always be present. */
#endif // BLE_DFU_APP_SUPPORT


static uint16_t                          m_conn_handle = BLE_CONN_HANDLE_INVALID;   /**< Handle of the current connection. */
static ble_hrs_t                         m_hrs;                                     /**< Structure used to identify the heart rate service. */



                                            
APP_TIMER_DEF(m_heart_rate_timer_id);                                               /**< Heart rate measurement timer. */

static dm_application_instance_t         m_app_handle;                              /**< Application identifier allocated by device manager */

static ble_uuid_t m_adv_uuids[] = {{BLE_UUID_HEART_RATE_SERVICE,BLE_UUID_TYPE_BLE}}; /**< Universally unique service identifiers. */
#ifdef BLE_DFU_APP_SUPPORT
static ble_dfu_t                         m_dfus;                                    /**< Structure used to identify the DFU service. */
#endif // BLE_DFU_APP_SUPPORT


/**@brief Callback function for asserts in the SoftDevice.
 *
 * @details This function will be called in case of an assert in the SoftDevice.
 *
 * @warning This handler is an example only and does not fit a final product. You need to analyze
 *          how your product is supposed to react in case of Assert.
 * @warning On assert from the SoftDevice, the system can only recover on reset.
 *
 * @param[in] line_num   Line number of the failing ASSERT call.
 * @param[in] file_name  File name of the failing ASSERT call.
 */
//void assert_nrf_callback(uint16_t line_num, const uint8_t * p_file_name)
//{
//    app_error_handler(DEAD_BEEF, line_num, p_file_name);
//}


// TWI (with transaction manager) initialization.

static void twi_config(void)
{
    uint32_t err_code;

    nrf_drv_twi_config_t const config = {
       .scl                = ARDUINO_SCL_PIN,
       .sda                = ARDUINO_SDA_PIN,
       .frequency          = NRF_TWI_FREQ_400K,
       .interrupt_priority = APP_IRQ_PRIORITY_HIGH,
    };
		
		err_code = nrf_drv_twi_init(&m_twi,&config,NULL,NULL);
		
    APP_ERROR_CHECK(err_code);
		
		nrf_drv_twi_enable(&m_twi);
}
// twi instance unintialiazation
static void twi_disable(void)
{
	
	  nrf_drv_twi_config_t const config = {
       .scl                = ARDUINO_SCL_PIN,
       .sda                = ARDUINO_SDA_PIN,
       .frequency          = NRF_TWI_FREQ_400K,
       .interrupt_priority = APP_IRQ_PRIORITY_HIGH,
    };
	
		
	 nrf_drv_twi_uninit(&m_twi,&config);
	 nrf_gpio_cfg_output(7);
	 nrf_gpio_cfg_output(30);
	 nrf_gpio_pin_clear(7);
	 nrf_gpio_pin_clear(30);
		
	
	 
}


// Accelerometer LIS2DH12 Configurion

void LIS2DH12_INIT()
{
	  ret_code_t err_code;
	
    uint8_t const LIS2DH12_CTRL_REG0[]      = {0x1E,0X90};             /*Pull up Disconnected to SA0 Pin*/
		uint8_t const LIS2DH12_TEMP_CFG_REG[]   = {0x1F,0X00};             /*Tempurature Sensor Disabled*/
	  uint8_t const LIS2DH12_CTRL_REG1[]      = {0x20,0X2F};             /*Low power mode with 10hz ODR and X,Y,Z axis enabled */
		uint8_t const LIS2DH12_CTRL_REG2[]      = {0x21,0X00};             /*High-pass filter enabled with normal mode.*/
		uint8_t const LIS2DH12_CTRL_REG3[]      = {0x22,0X00};             /*Disable all interupts.*/
		uint8_t const LIS2DH12_CTRL_REG4[]      = {0x23,0X24};             /*Full Scale = +/-8 g with BDU enabled and HR Disable.Test enabled*/
    uint8_t const LIS2DH12_CTRL_REG5[]      = {0x24,0X40};             /*Enable FIFO buffer*/
		uint8_t const LIS2DH12_CTRL_REG6[]      = {0x25,0X00};             /**/
		uint8_t const LIS2DH12_FIFO_CTRL_REG[]  = {0x2E,0x40};             /*Enable the FIFO mode*/		

//		
 //configure the LIS2DH12 oxilarometer sensor
		
		err_code=nrf_drv_twi_tx(&m_twi,LIS2DH12_ADDRESS,LIS2DH12_CTRL_REG0,sizeof(LIS2DH12_CTRL_REG0),false);
		APP_ERROR_CHECK(err_code);
		err_code=nrf_drv_twi_tx(&m_twi,LIS2DH12_ADDRESS,LIS2DH12_TEMP_CFG_REG,sizeof(LIS2DH12_TEMP_CFG_REG),false);
		APP_ERROR_CHECK(err_code);
		err_code=nrf_drv_twi_tx(&m_twi,LIS2DH12_ADDRESS,LIS2DH12_CTRL_REG1,sizeof(LIS2DH12_CTRL_REG1),false);
		APP_ERROR_CHECK(err_code);
		err_code=nrf_drv_twi_tx(&m_twi,LIS2DH12_ADDRESS,LIS2DH12_CTRL_REG2,sizeof(LIS2DH12_CTRL_REG2),false);
		APP_ERROR_CHECK(err_code);
		err_code=nrf_drv_twi_tx(&m_twi,LIS2DH12_ADDRESS,LIS2DH12_CTRL_REG3,sizeof(LIS2DH12_CTRL_REG3),false);
		APP_ERROR_CHECK(err_code);
		err_code=nrf_drv_twi_tx(&m_twi,LIS2DH12_ADDRESS,LIS2DH12_CTRL_REG4,sizeof(LIS2DH12_CTRL_REG4),false);
		APP_ERROR_CHECK(err_code);
		err_code=nrf_drv_twi_tx(&m_twi,LIS2DH12_ADDRESS,LIS2DH12_CTRL_REG5,sizeof(LIS2DH12_CTRL_REG5),false);
		APP_ERROR_CHECK(err_code);
		err_code=nrf_drv_twi_tx(&m_twi,LIS2DH12_ADDRESS,LIS2DH12_CTRL_REG6,sizeof(LIS2DH12_CTRL_REG6),false);
		APP_ERROR_CHECK(err_code);
		err_code=nrf_drv_twi_tx(&m_twi,LIS2DH12_ADDRESS,LIS2DH12_FIFO_CTRL_REG,sizeof(LIS2DH12_FIFO_CTRL_REG),false);
		APP_ERROR_CHECK(err_code);
		
		
 }

 void LIS2DH12_XYZ_read()
{
		ret_code_t err_code;
		
		uint8_t const  acc_xout_l=0x28;
		uint8_t const acc_xout_h=0x29;

		uint8_t const acc_yout_l=0x2A;
		uint8_t const acc_yout_h=0x2B;

		uint8_t const acc_zout_l=0x2C;
		uint8_t const acc_zout_h=0x2D;

		err_code = nrf_drv_twi_tx(&m_twi,LIS2DH12_ADDRESS,&acc_xout_l,1,false);
		APP_ERROR_CHECK(err_code);

		err_code = nrf_drv_twi_rx(&m_twi,LIS2DH12_ADDRESS,&m_buffer[6],1);
		APP_ERROR_CHECK(err_code);
					 
		err_code = nrf_drv_twi_tx(&m_twi,LIS2DH12_ADDRESS,&acc_xout_h,1,false);
		APP_ERROR_CHECK(err_code);
							 
		err_code = nrf_drv_twi_rx(&m_twi, LIS2DH12_ADDRESS,&m_buffer[7],1);
		APP_ERROR_CHECK(err_code);
						
		err_code = nrf_drv_twi_tx(&m_twi,LIS2DH12_ADDRESS,&acc_yout_l,1,false);
		APP_ERROR_CHECK(err_code);
							 
		err_code = nrf_drv_twi_rx(&m_twi,LIS2DH12_ADDRESS,&m_buffer[8],1);
		APP_ERROR_CHECK(err_code);
							 
		err_code = nrf_drv_twi_tx(&m_twi,LIS2DH12_ADDRESS,&acc_yout_h,1,false);
		APP_ERROR_CHECK(err_code);
							 
		err_code = nrf_drv_twi_rx(&m_twi, LIS2DH12_ADDRESS,&m_buffer[9],1);
		APP_ERROR_CHECK(err_code);
		
		err_code = nrf_drv_twi_tx(&m_twi,LIS2DH12_ADDRESS,&acc_zout_l,1,false);
		APP_ERROR_CHECK(err_code);
							 
		err_code = nrf_drv_twi_rx(&m_twi, LIS2DH12_ADDRESS,&m_buffer[10],1);
		APP_ERROR_CHECK(err_code);
							 
		err_code = nrf_drv_twi_tx(&m_twi,LIS2DH12_ADDRESS,&acc_zout_h,1,false);
		APP_ERROR_CHECK(err_code);
							 
		err_code = nrf_drv_twi_rx(&m_twi, LIS2DH12_ADDRESS,&m_buffer[11],1);
		APP_ERROR_CHECK(err_code);
}

void  MAX30102_INIT()
{
	 ret_code_t err_code;
	 uint8_t const fifo_en_config[]   = {0x08,0x10};//FIFO register configuration 
   uint8_t const mode_en_config[]   = {0x09,0x03};//SP02 Mode configuration 
	 uint8_t const spo2_en_config[]   = {0x0A,0x15};//SPO2 configurarion
	 uint8_t const led1_en_config[]   = {0x0C,0x0C};//RED led current configuration
	 uint8_t const led2_en_config[]   = {0x0D,0x0C};//IR led current configuration
	 uint8_t const proxi_en_config[]  = {0x10,0xFF};//
	 uint8_t const led1_slot_config[] = {0x11,0x02};
	 uint8_t const led2_slot_config[] = {0x12,0x02};
	 uint8_t const temp_config[]      = {0x21,0x01};
	 uint8_t const prox_int_config[] = {0x30,0xff};
	 
	//  	 reset_fifo();
		//initialize the max30100 pulse oximetre sensor
		   err_code=nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,mode_en_config,sizeof(mode_en_config),false);
	     nrf_delay_us(3);
	     APP_ERROR_CHECK(err_code); 
    	 nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,fifo_en_config,sizeof(fifo_en_config),false);
	     nrf_delay_us(3);
	     APP_ERROR_CHECK(err_code);
    	 nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,spo2_en_config,sizeof(spo2_en_config),false);
	     nrf_delay_us(3);
	     APP_ERROR_CHECK(err_code);
			 nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,led1_en_config,sizeof(led1_en_config),false);
	     nrf_delay_us(3);
	     APP_ERROR_CHECK(err_code);
		   nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,led2_en_config,sizeof(led2_en_config),false);
	     nrf_delay_us(3);
	     APP_ERROR_CHECK(err_code);
	     nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,proxi_en_config,sizeof(proxi_en_config),false);
	     nrf_delay_us(3);
	     APP_ERROR_CHECK(err_code);
       nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,led1_slot_config,sizeof(led1_slot_config),false);
			 nrf_delay_us(3);
	     APP_ERROR_CHECK(err_code);
	     nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,led2_slot_config,sizeof(led2_slot_config),false);
			 nrf_delay_us(3);
	     APP_ERROR_CHECK(err_code);
	     nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,temp_config,sizeof(temp_config),false);
			 nrf_delay_us(3);
	     APP_ERROR_CHECK(err_code);
	     nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,prox_int_config,sizeof(prox_int_config),false);
			 nrf_delay_us(3);
			 APP_ERROR_CHECK(err_code);

}


#ifdef BLE_DFU_APP_SUPPORT
/**@brief Function for stopping advertising.
 */
static void advertising_stop(void)
{
    uint32_t err_code;

    err_code = sd_ble_gap_adv_stop();
    APP_ERROR_CHECK(err_code);

    err_code = bsp_indication_set(BSP_INDICATE_IDLE);
    APP_ERROR_CHECK(err_code);
}
/**@brief Function for handling the Heart rate measurement timer timeout.
 *
 * @details This function will be called each time the heart rate measurement timer expires.
 *          It will exclude RR Interval data from every third measurement.
 *
 * @param[in] p_context  Pointer used for passing some arbitrary information (context) from the
 *                       app_start_timer() call to the timeout handler.
 */
static void heart_rate_meas_timeout_handler(void *p_context)
{
	
    static uint32_t i = 0;
    uint32_t        err_code;
    uint32_t        heart_rate=72;
	  uint32_t spo2_value=94;
	  uint8_t sensor_data[PACKET_LEN] = {0};
	  uint16_t temp_value=31;
		uint8_t const reg_address=0x07;
		uint8_t const mode_dis_config[] = { 0x06,0x80};
	  uint8_t const reg_fifo_rd_address[2]={0x04,0x0F};
	  uint8_t const reg_temp_int_address=0x1F;
		uint8_t const reg_temp_frac_address=0x20;
		uint32_t spo2;
		
		
		epoch_time++;
		
		sensor_en_time++;
		
		
		 
		 if(!data_store_flag)
		 {		 
				if(sensor_wakeup_time_update_flag)
				{
					if(sensor_en_time>sensor_wakeup_time)
					{
						 sensor_en_time=0;
					}
				 sensor_wakeup_time_update_flag=false;
				}
		 }
		
		 if(sensor_en_time==sensor_wakeup_time)
			 {
 		     sensor_en_flag=true;
		     
       }
			 
		  if((sensor_en_time)>sensor_wakeup_time)
				{
					   uint8_t const Temp_en_config[] = {0x21,0x01};						 /* SP02 mode and Enable tempurature Sensor every time for updated value */ 
						 uint8_t const mode_en_config[]   = {0x09,0x03};    
						 data_store_flag=true;
						 sensor_dis_time++; 	
					   err_code=nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,mode_en_config,sizeof(mode_en_config),false);
             APP_ERROR_CHECK(err_code);
						 err_code=nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,Temp_en_config,sizeof(mode_en_config),false);
             APP_ERROR_CHECK(err_code);
				}
			 
				 
					 
			if(sensor_en_flag)
		  {	
			   nrf_gpio_cfg_output(12);
				 nrf_delay_ms(5);
				
			   nrf_gpio_pin_set(12);
				 nrf_delay_ms(5);
		   
			   twi_config(); 
			   nrf_delay_ms(5);
				
         sensor_en_flag=false;	
			
			 		uint8_t const mode_en_config[] = {0x06,0x0B};
					uint8_t const spo2_en_config[] = {0x07,0x01};                  
		
		//initialize the max30100 pulse oximetre sensor
				 MAX30102_INIT();
		//Accelorometer Intialization
				 LIS2DH12_INIT();
		}	
			
		
		
			if(sensor_dis_time>sensor_active_time)
			{
			  sensor_dis_flag=true; 
				
				sensor_dis_time=0;
			}
			
			if(sensor_en_time>(sensor_wakeup_time+sensor_active_time))
			{
				sensor_en_time=1;
			}
			
			if(sensor_dis_flag)
			{
				 
				 uint8_t dummy_buffer[PACKET_LEN]={0x00};
				 dummy_buffer[0]=0x01;
				 dummy_buffer[1] =(uint8_t)(epoch_time>>24); 
				 dummy_buffer[2] =(uint8_t)(epoch_time>>16);
				 dummy_buffer[3] =(uint8_t)(epoch_time>>8);
				 dummy_buffer[4]	=(uint8_t)epoch_time;
						;
				 sensor_dis_flag=false;
			   err_code=nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,mode_dis_config,sizeof(mode_dis_config),false);
				 APP_ERROR_CHECK(err_code);
				 
				 err_code = ble_hrs_heart_rate_measurement_send(&m_hrs,&dummy_buffer[0],PACKET_LEN);
				 if ((err_code != NRF_SUCCESS) &&
             (err_code != NRF_ERROR_INVALID_STATE) &&
             (err_code != BLE_ERROR_NO_TX_PACKETS) &&
             (err_code != BLE_ERROR_GATTS_SYS_ATTR_MISSING)
            )
                {
                  APP_ERROR_HANDLER(err_code);
                }
				 
				 twi_disable();
				 nrf_delay_ms(10);
								
				 data_store_flag=false;
								
				 nrf_gpio_cfg_output(12);
				 nrf_delay_ms(5);
								
         nrf_gpio_pin_clear(12);
				 nrf_delay_ms(5);			
		  	  
	  
        	
			}
			
			if(data_store_flag)
			{
					
				 err_code = nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,reg_fifo_rd_address,2,false);
				 APP_ERROR_CHECK(err_code);
				 err_code = nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,&reg_address,1,false);
				 APP_ERROR_CHECK(err_code);
				 err_code = nrf_drv_twi_rx(&m_twi,MAX30100_ADDRESS,m_buffer,6);
				 APP_ERROR_CHECK(err_code);
				 err_code = nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,&reg_temp_int_address,1,false);
				 APP_ERROR_CHECK(err_code); 
				 err_code = nrf_drv_twi_rx(&m_twi,MAX30100_ADDRESS,&m_buffer[6],1);
				 APP_ERROR_CHECK(err_code);
				 err_code = nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,&reg_temp_frac_address,1,false);
				 APP_ERROR_CHECK(err_code);
				 err_code = nrf_drv_twi_rx(&m_twi,MAX30100_ADDRESS,&m_buffer[7],1);
				 APP_ERROR_CHECK(err_code);
			
				
			
					heart_rate=MAX30100_GET_HEART_RATE_VALUE(m_buffer[0],m_buffer[1],m_buffer[2]);		    
				  spo2_value=MAX30100_GET_SPO2_VALUE(m_buffer[3],m_buffer[4],m_buffer[5]);
					temp_value=MAX30100_GET_TEMPERATURE_VALUE(m_buffer[6], m_buffer[7]);

		
				 LIS2DH12_XYZ_read();
								 
								 
								 //Fill timestamp
				 sensor_data[0] =0x00;
				 sensor_data[1] =(uint8_t)(epoch_time>>24); 
				 sensor_data[2] =(uint8_t)(epoch_time>>16);
				 sensor_data[3] =(uint8_t)(epoch_time>>8);
				 sensor_data[4]	=(uint8_t)epoch_time;
						
			//Fill sensor value
				 sensor_data[5]	= (uint8_t)(heart_rate<<8);
				 sensor_data[6]	= (uint8_t)heart_rate;
				 sensor_data[7]	= (uint8_t)(spo2_value<<8);
				 sensor_data[8]	= (uint8_t)spo2_value;
				 sensor_data[9]	= (uint8_t)(temp_value<<8);
				 sensor_data[10]	= (uint8_t)temp_value;
				 
			 // Fill the accelorometer x,y,z values
					sensor_data[11]=(uint8_t)m_buffer[6];
					sensor_data[12]=(uint8_t)m_buffer[7];
					sensor_data[13]=(uint8_t)m_buffer[8];
					sensor_data[14]=(uint8_t)m_buffer[9];
					sensor_data[15]=(uint8_t)m_buffer[10];
					sensor_data[16]=(uint8_t)m_buffer[11];
					
				
    

        
				if((Device_Connection_State)&&(Live_data_flag))
					{
						sensor_data[0] =0x01; 
						err_code=ble_hrs_heart_rate_measurement_send(&m_hrs,&sensor_data[0],PACKET_LEN);
			      if ((err_code != NRF_SUCCESS) &&
                (err_code != NRF_ERROR_INVALID_STATE) &&
                (err_code != BLE_ERROR_NO_TX_PACKETS) &&
                (err_code != BLE_ERROR_GATTS_SYS_ATTR_MISSING)
               )
                {
                  APP_ERROR_HANDLER(err_code);
                }
					}  
           

		   if((m_sample_write_idx)<=(NUMBER_OF_SAMPLES-1)&&(data_store_flag))                                            //&&(sensor_data_rx_flag))                               //&&(tx_state!=DATA))
	       {
					   sensor_data[0] =0x00; 
					   
					   for(i=0;i<PACKET_LEN;i++)
              {					 
                     m_samples[m_sample_write_idx]=	sensor_data[i];
							       m_sample_write_idx++;
						  }
				 }
      
			 
				 
				 if(m_sample_write_idx>=NUMBER_OF_SAMPLES)
				   {
					     m_sample_write_idx=0;
						   over_write_flag=true;
				   }
							     
		
		
					 
        UNUSED_PARAMETER(p_context);
					 
					
  

					 
					 
		
		memset(&m_buffer, 0, sizeof(m_buffer));
		
	 }

}

	



/**@brief Function for the Timer initialization.
 *
 * @details Initializes the timer module. This creates and starts application timers.
 */
static void timers_init(void)
{
    uint32_t err_code;

    // Initialize timer module.
    APP_TIMER_INIT(APP_TIMER_PRESCALER, APP_TIMER_OP_QUEUE_SIZE, false);

    // Create timers.
    err_code = app_timer_create(&m_heart_rate_timer_id,
                                APP_TIMER_MODE_REPEATED,
                                heart_rate_meas_timeout_handler);
    APP_ERROR_CHECK(err_code);
	


}


/**@brief Function for the GAP initialization.
 *
 * @details This function sets up all the necessary GAP (Generic Access Profile) parameters of the
 *          device including the device name, appearance, and the preferred connection parameters.
 */
static void gap_params_init(void)
{
    uint32_t                err_code;
    ble_gap_conn_params_t   gap_conn_params;
    ble_gap_conn_sec_mode_t sec_mode;

    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&sec_mode);
	  

    err_code = sd_ble_gap_device_name_set(&sec_mode,
                                          (const uint8_t*)device_name,
                                          strlen((const char*)device_name));
    APP_ERROR_CHECK(err_code);

    err_code = sd_ble_gap_appearance_set(BLE_APPEARANCE_GENERIC_HEART_RATE_SENSOR);
																					
    APP_ERROR_CHECK(err_code);
																					
	  																	
    memset(&gap_conn_params, 0, sizeof(gap_conn_params));

    gap_conn_params.min_conn_interval = MIN_CONN_INTERVAL;
    gap_conn_params.max_conn_interval = MAX_CONN_INTERVAL;
    gap_conn_params.slave_latency     = SLAVE_LATENCY;
    gap_conn_params.conn_sup_timeout  = CONN_SUP_TIMEOUT;

    err_code = sd_ble_gap_ppcp_set(&gap_conn_params);
    APP_ERROR_CHECK(err_code);
}





/**@brief Function for loading application-specific context after establishing a secure connection.
 *
 * @details This function will load the application context and check if the ATT table is marked as
 *          changed. If the ATT table is marked as changed, a Service Changed Indication
 *          is sent to the peer if the Service Changed CCCD is set to indicate.
 *
 * @param[in] p_handle The Device Manager handle that identifies the connection for which the context
 *                     should be loaded.
 */
static void app_context_load(dm_handle_t const * p_handle)
{
    uint32_t                 err_code;
    static uint32_t          context_data;
    dm_application_context_t context;

    context.len    = sizeof(context_data);
    context.p_data = (uint8_t *)&context_data;

    err_code = dm_application_context_get(p_handle, &context);
    if (err_code == NRF_SUCCESS)
    {
        // Send Service Changed Indication if ATT table has changed.
        if ((context_data & (DFU_APP_ATT_TABLE_CHANGED << DFU_APP_ATT_TABLE_POS)) != 0)
        {
            err_code = sd_ble_gatts_service_changed(m_conn_handle, APP_SERVICE_HANDLE_START, BLE_HANDLE_MAX);
            if ((err_code != NRF_SUCCESS) &&
                (err_code != BLE_ERROR_INVALID_CONN_HANDLE) &&
                (err_code != NRF_ERROR_INVALID_STATE) &&
                (err_code != BLE_ERROR_NO_TX_PACKETS) &&
                (err_code != NRF_ERROR_BUSY) &&
                (err_code != BLE_ERROR_GATTS_SYS_ATTR_MISSING))
            {
                APP_ERROR_HANDLER(err_code);
            }
        }

        err_code = dm_application_context_delete(p_handle);
        APP_ERROR_CHECK(err_code);
    }
    else if (err_code == DM_NO_APP_CONTEXT)
    {
        // No context available. Ignore.
    }
    else
    {
        APP_ERROR_HANDLER(err_code);
    }
}


/** @snippet [DFU BLE Reset prepare] */
/**@brief Function for preparing for system reset.
 *
 * @details This function implements @ref dfu_app_reset_prepare_t. It will be called by
 *          @ref dfu_app_handler.c before entering the bootloader/DFU.
 *          This allows the current running application to shut down gracefully.
 */
static void reset_prepare(void)
{
    uint32_t err_code;

    if (m_conn_handle != BLE_CONN_HANDLE_INVALID)
    {
        // Disconnect from peer.
        err_code = sd_ble_gap_disconnect(m_conn_handle, BLE_HCI_REMOTE_USER_TERMINATED_CONNECTION);
        APP_ERROR_CHECK(err_code);
        err_code = bsp_indication_set(BSP_INDICATE_IDLE);
        APP_ERROR_CHECK(err_code);
    }
    else
    {
        // If not connected, the device will be advertising. Hence stop the advertising.
        advertising_stop();
    }

    err_code = ble_conn_params_stop();
    APP_ERROR_CHECK(err_code);

    nrf_delay_ms(500);
}
/** @snippet [DFU BLE Reset prepare] */
#endif // BLE_DFU_APP_SUPPORT


/**@brief Function for initializing services that will be used by the application.
 *
 * @details Initialize the Heart Rate, Battery and Device Information services.
 */
static void services_init(void)
{
    uint32_t       err_code;
    ble_hrs_init_t hrs_init;
    uint8_t        body_sensor_location;

    // Initialize Heart Rate Service.
    body_sensor_location = BLE_HRS_BODY_SENSOR_LOCATION_FINGER;

    memset(&hrs_init, 0, sizeof(hrs_init));

    hrs_init.evt_handler                 = NULL;
    hrs_init.is_sensor_contact_supported = true;
    hrs_init.p_body_sensor_location      = &body_sensor_location;

    // Here the sec level for the Heart Rate Service can be changed/increased.
    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&hrs_init.hrs_hrm_attr_md.cccd_write_perm);
    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&hrs_init.hrs_hrm_attr_md.read_perm);
    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&hrs_init.hrs_hrm_attr_md.write_perm);

    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&hrs_init.hrs_bsl_attr_md.read_perm);
    BLE_GAP_CONN_SEC_MODE_SET_NO_ACCESS(&hrs_init.hrs_bsl_attr_md.write_perm);

    err_code = ble_hrs_init(&m_hrs,&hrs_init);
    APP_ERROR_CHECK(err_code);

#ifdef BLE_DFU_APP_SUPPORT
    /** @snippet [DFU BLE Service initialization] */
    ble_dfu_init_t   dfus_init;

    // Initialize the Device Firmware Update Service.
    memset(&dfus_init, 0, sizeof(dfus_init));

    dfus_init.evt_handler   = dfu_app_on_dfu_evt;
    dfus_init.error_handler = NULL;
    dfus_init.evt_handler   = dfu_app_on_dfu_evt;
    dfus_init.revision      = DFU_REVISION;

    err_code = ble_dfu_init(&m_dfus, &dfus_init);
    APP_ERROR_CHECK(err_code);

    dfu_app_reset_prepare_set(reset_prepare);
    dfu_app_dm_appl_instance_set(m_app_handle);
    /** @snippet [DFU BLE Service initialization] */
#endif // BLE_DFU_APP_SUPPORT
}


/**@brief Function for starting application timers.
 */
static void application_timers_start(void)
{
    uint32_t err_code;

    // Start application timers.

    err_code = app_timer_start(m_heart_rate_timer_id, HEART_RATE_MEAS_INTERVAL, NULL);
    APP_ERROR_CHECK(err_code);

}


/**@brief Function for handling the Connection Parameters Module.
 *
 * @details This function will be called for all events in the Connection Parameters Module which
 *          are passed to the application.
 *          @note All this function does is to disconnect. This could have been done by simply
 *                setting the disconnect_on_fail config parameter, but instead we use the event
 *                handler mechanism to demonstrate its use.
 *
 * @param[in] p_evt  Event received from the Connection Parameters Module.
 */
static void on_conn_params_evt(ble_conn_params_evt_t * p_evt)
{
    uint32_t err_code;

    if (p_evt->evt_type == BLE_CONN_PARAMS_EVT_FAILED)
    {
        err_code = sd_ble_gap_disconnect(m_conn_handle, BLE_HCI_CONN_INTERVAL_UNACCEPTABLE);
        APP_ERROR_CHECK(err_code);
    }
}


/**@brief Function for handling a Connection Parameters error.
 *
 * @param[in] nrf_error  Error code containing information about what went wrong.
 */
static void conn_params_error_handler(uint32_t nrf_error)
{
    APP_ERROR_HANDLER(nrf_error);
}


/**@brief Function for initializing the Connection Parameters module.
 */
static void conn_params_init(void)
{
    uint32_t               err_code;
    ble_conn_params_init_t cp_init;

    memset(&cp_init, 0, sizeof(cp_init));

    cp_init.p_conn_params                  = NULL;
    cp_init.first_conn_params_update_delay = FIRST_CONN_PARAMS_UPDATE_DELAY;
    cp_init.next_conn_params_update_delay  = NEXT_CONN_PARAMS_UPDATE_DELAY;
    cp_init.max_conn_params_update_count   = MAX_CONN_PARAMS_UPDATE_COUNT;
    cp_init.start_on_notify_cccd_handle    = m_hrs.hrm_handles.cccd_handle;
    cp_init.disconnect_on_fail             = false;
    cp_init.evt_handler                    = on_conn_params_evt;
    cp_init.error_handler                  = conn_params_error_handler;

    err_code = ble_conn_params_init(&cp_init);
    APP_ERROR_CHECK(err_code);
}


/**@brief Function for putting the chip into sleep mode.
 *
 * @note This function will not return.
 */
static void sleep_mode_enter(void)
{
    uint32_t err_code = bsp_indication_set(BSP_INDICATE_IDLE);
    APP_ERROR_CHECK(err_code);

    // Prepare wakeup buttons.
    err_code = bsp_btn_ble_sleep_mode_prepare();
    APP_ERROR_CHECK(err_code);

    // Go to system-off mode (this function will not return; wakeup will cause a reset).
    err_code = sd_power_system_off();
    APP_ERROR_CHECK(err_code);
}


/**@brief Function for handling advertising events.
 *
 * @details This function will be called for advertising events which are passed to the application.
 *
 * @param[in] ble_adv_evt  Advertising event.
 */
static void on_adv_evt(ble_adv_evt_t ble_adv_evt)
{
    uint32_t err_code;

    switch (ble_adv_evt) 
    {
        case BLE_ADV_EVT_FAST:
            err_code = bsp_indication_set(BSP_INDICATE_ADVERTISING);
            APP_ERROR_CHECK(err_code);
            break;
        case BLE_ADV_EVT_IDLE:
            sleep_mode_enter();
            break;
        default:
            break;
    }
}


/**@brief Function for handling the Application's BLE Stack events.
 *
 * @param[in] p_ble_evt  Bluetooth stack event.
 */
static void on_ble_evt(ble_evt_t * p_ble_evt)
{
             uint32_t                err_code;
             ble_gap_conn_params_t   gap_conn_params;
             ble_gap_conn_sec_mode_t sec_mode;


    switch (p_ble_evt->header.evt_id)
            {
        case BLE_GAP_EVT_CONNECTED:
          //   err_code = bsp_indication_set(BSP_INDICATE_CONNECTED);
            // APP_ERROR_CHECK(err_code);
             m_conn_handle = p_ble_evt->evt.gap_evt.conn_handle;
				     Device_Connection_State=true;
             break;
         
        case BLE_GAP_EVT_DISCONNECTED:
					   
				     
             BLE_GAP_CONN_SEC_MODE_SET_OPEN(&sec_mode);

             err_code = sd_ble_gap_device_name_set(&sec_mode,
                                             (const uint8_t*)device_name,
                                                    strlen((const char*)device_name));//strlen((const char*)DEVICE_NAME));
             if(err_code != NRF_SUCCESS)
						  {
						        APP_ERROR_CHECK(err_code);
					  	}
						
						advertising_init();
						
					  err_code = ble_advertising_start(BLE_ADV_MODE_DIRECTED);
						Device_Connection_State=false;
						Live_data_flag=false;
						if(err_code != NRF_SUCCESS)
						{
					      	APP_ERROR_CHECK(err_code);
						}
			     
					  m_conn_handle = BLE_CONN_HANDLE_INVALID;
            break;
				case BLE_EVT_TX_COMPLETE:
					
				     //Data sending from starting of the FIFO to 
				    	if((data_tx_flag)&&(!over_write_flag)&&(m_sample_read_idx<=m_sample_write_idx)&&((m_sample_read_idx!=m_sample_write_idx)))
							{
                err_code = ble_hrs_heart_rate_measurement_send(&m_hrs,&m_samples[m_sample_read_idx],PACKET_LEN);
						    if((err_code != NRF_SUCCESS) &&
                   (err_code != NRF_ERROR_INVALID_STATE) &&
                   (err_code != BLE_ERROR_NO_TX_PACKETS) &&
                   (err_code != BLE_ERROR_GATTS_SYS_ATTR_MISSING)
                  )
                  {
                    APP_ERROR_HANDLER(err_code);
                  }
								m_sample_read_idx = m_sample_read_idx+PACKET_LEN;
						  } 
	  					
							//checking the condition for write pointer ahead of read ponter 
							
						 else if((data_tx_flag)&&(over_write_flag)&&(m_sample_write_idx>m_sample_read_idx)&&((m_sample_read_idx!=m_sample_write_idx)))
						 {
								 m_sample_read_idx=m_sample_write_idx;
							   err_code = ble_hrs_heart_rate_measurement_send(&m_hrs,&m_samples[m_sample_read_idx],PACKET_LEN);
				         if ((err_code != NRF_SUCCESS) &&
                     (err_code != NRF_ERROR_INVALID_STATE) &&
                     (err_code != BLE_ERROR_NO_TX_PACKETS) &&
                     (err_code != BLE_ERROR_GATTS_SYS_ATTR_MISSING)
                    )
                     {
                       APP_ERROR_HANDLER(err_code);
                     }
							   m_sample_read_idx = m_sample_read_idx+PACKET_LEN;	
						 }
             else if((data_tx_flag)&&(over_write_flag)&&(m_sample_read_idx<=NUMBER_OF_SAMPLES)&&(m_sample_read_idx!=m_sample_write_idx))
						  {
							  err_code = ble_hrs_heart_rate_measurement_send(&m_hrs,&m_samples[m_sample_read_idx],PACKET_LEN);
						    if ((err_code != NRF_SUCCESS) &&
                    (err_code != NRF_ERROR_INVALID_STATE) &&
                    (err_code != BLE_ERROR_NO_TX_PACKETS) &&
                    (err_code != BLE_ERROR_GATTS_SYS_ATTR_MISSING)
                   )
                   {
                     APP_ERROR_HANDLER(err_code);
                   }
							  m_sample_read_idx = m_sample_read_idx+PACKET_LEN;	
              }
				     else 
							{
							     data_tx_flag=false;
								   sensor_data_rx_flag=true;
					         Live_data_flag=true;
						  }
							
								
							if(m_sample_read_idx>NUMBER_OF_SAMPLES)
						    {
						        m_sample_read_idx = 0;
									  over_write_flag=false;
					      }		
							
							
	    			break;
				
				    
				
				 
					   

        default:
            // No implementation needed.
            break;
    }
}


/**@brief Function for dispatching a BLE stack event to all modules with a BLE stack event handler.
 *
 * @details This function is called from the BLE Stack event interrupt handler after a BLE stack
 *          event has been received.
 *
 * @param[in] p_ble_evt  Bluetooth stack event.
 */
static void ble_evt_dispatch(ble_evt_t * p_ble_evt)
{
    dm_ble_evt_handler(p_ble_evt);
    ble_hrs_on_ble_evt(&m_hrs, p_ble_evt);
   // ble_bas_on_ble_evt(&m_bas, p_ble_evt);
    ble_conn_params_on_ble_evt(p_ble_evt);
   // bsp_btn_ble_on_ble_evt(p_ble_evt);
#ifdef BLE_DFU_APP_SUPPORT
    /** @snippet [Propagating BLE Stack events to DFU Service] */
    ble_dfu_on_ble_evt(&m_dfus, p_ble_evt);
    /** @snippet [Propagating BLE Stack events to DFU Service] */
#endif // BLE_DFU_APP_SUPPORT
    on_ble_evt(p_ble_evt);
    ble_advertising_on_ble_evt(p_ble_evt);
}


/**@brief Function for dispatching a system event to interested modules.
 *
 * @details This function is called from the System event interrupt handler after a system
 *          event has been received.
 *
 * @param[in] sys_evt  System stack event.
 */
static void sys_evt_dispatch(uint32_t sys_evt)
{
    pstorage_sys_event_handler(sys_evt);
    ble_advertising_on_sys_evt(sys_evt);
}


/**@brief Function for initializing the BLE stack.
 *
 * @details Initializes the SoftDevice and the BLE event interrupt.
 */
static void ble_stack_init(void)
{
    uint32_t err_code;
    
    nrf_clock_lf_cfg_t clock_lf_cfg = NRF_CLOCK_LFCLKSRC;
    
    // Initialize the SoftDevice handler module.	
    SOFTDEVICE_HANDLER_INIT(&clock_lf_cfg, NULL);

    ble_enable_params_t ble_enable_params;
    err_code = softdevice_enable_get_default_config(CENTRAL_LINK_COUNT,
                                                    PERIPHERAL_LINK_COUNT,
                                                    &ble_enable_params);
    APP_ERROR_CHECK(err_code);

#ifdef BLE_DFU_APP_SUPPORT
    ble_enable_params.gatts_enable_params.service_changed = 1;
#endif // BLE_DFU_APP_SUPPORT
    //Check the ram settings against the used number of links
    CHECK_RAM_START_ADDR(CENTRAL_LINK_COUNT,PERIPHERAL_LINK_COUNT);

    // Enable BLE stack.
    err_code = softdevice_enable(&ble_enable_params);
    APP_ERROR_CHECK(err_code);

    // Register with the SoftDevice handler module for BLE events.
    err_code = softdevice_ble_evt_handler_set(ble_evt_dispatch); 
    APP_ERROR_CHECK(err_code);

    // Register with the SoftDevice handler module for BLE events.
    err_code = softdevice_sys_evt_handler_set(sys_evt_dispatch);
    APP_ERROR_CHECK(err_code);
}


/**@brief Function for handling events from the BSP module.
 *
 * @param[in]   event   Event generated by button press.
 */


/**@brief Function for handling the Device Manager events.
 *
 * @param[in] p_evt  Data associated to the device manager event.
 */
static uint32_t device_manager_evt_handler(dm_handle_t const * p_handle,
                                           dm_event_t const  * p_event,
                                           ret_code_t        event_result)
{
    APP_ERROR_CHECK(event_result);

#ifdef BLE_DFU_APP_SUPPORT
    if (p_event->event_id == DM_EVT_LINK_SECURED)
    {
        app_context_load(p_handle);
    }
#endif // BLE_DFU_APP_SUPPORT

    return NRF_SUCCESS;
}


/**@brief Function for the Device Manager initialization.
 *
 * @param[in] erase_bonds  Indicates whether bonding information should be cleared from
 *                         persistent storage during initialization of the Device Manager.
 */
static void device_manager_init(bool erase_bonds)
{
    uint32_t               err_code;
    dm_init_param_t        init_param = {.clear_persistent_data = erase_bonds};
    dm_application_param_t register_param;

    // Initialize persistent storage module.
    err_code = pstorage_init();
    APP_ERROR_CHECK(err_code);

    err_code = dm_init(&init_param);
    APP_ERROR_CHECK(err_code);

    memset(&register_param.sec_param, 0, sizeof(ble_gap_sec_params_t));

    register_param.sec_param.bond         = SEC_PARAM_BOND;
    register_param.sec_param.mitm         = SEC_PARAM_MITM;
    register_param.sec_param.lesc         = SEC_PARAM_LESC;
    register_param.sec_param.keypress     = SEC_PARAM_KEYPRESS;
    register_param.sec_param.io_caps      = SEC_PARAM_IO_CAPABILITIES;
    register_param.sec_param.oob          = SEC_PARAM_OOB;
    register_param.sec_param.min_key_size = SEC_PARAM_MIN_KEY_SIZE;
    register_param.sec_param.max_key_size = SEC_PARAM_MAX_KEY_SIZE;
    register_param.evt_handler            = device_manager_evt_handler;
    register_param.service_type           = DM_PROTOCOL_CNTXT_GATT_SRVR_ID;

    err_code = dm_register(&m_app_handle, &register_param);
    APP_ERROR_CHECK(err_code);
}


/**@brief Function for initializing the Advertising functionality.
 */
static void advertising_init(void)
{
    uint32_t      err_code;
    ble_advdata_t advdata;

    // Build advertising data struct to pass into @ref ble_advertising_init.
    memset(&advdata, 0, sizeof(advdata));

    advdata.name_type               = BLE_ADVDATA_FULL_NAME;
    advdata.include_appearance      = true;
    advdata.flags                   = BLE_GAP_ADV_FLAGS_LE_ONLY_GENERAL_DISC_MODE;
    advdata.uuids_complete.uuid_cnt = sizeof(m_adv_uuids) / sizeof(m_adv_uuids[0]);
    advdata.uuids_complete.p_uuids  = m_adv_uuids;

    ble_adv_modes_config_t options = {0};
    options.ble_adv_fast_enabled  = BLE_ADV_FAST_ENABLED;
    options.ble_adv_fast_interval = adv_interval;
    options.ble_adv_fast_timeout  = APP_ADV_TIMEOUT_IN_SECONDS;

    err_code = ble_advertising_init(&advdata, NULL, &options, on_adv_evt, NULL);
    APP_ERROR_CHECK(err_code);
}

void bsp_event_handler(bsp_event_t event)
{
    uint32_t err_code;

    switch (event)
    {
        case BSP_EVENT_SLEEP:
            sleep_mode_enter();
            break;

        case BSP_EVENT_DISCONNECT:
            err_code = sd_ble_gap_disconnect(m_conn_handle,
                                             BLE_HCI_REMOTE_USER_TERMINATED_CONNECTION);
            if (err_code != NRF_ERROR_INVALID_STATE)
            {
                APP_ERROR_CHECK(err_code);
            }
            break;

        case BSP_EVENT_WHITELIST_OFF:
            if (m_conn_handle == BLE_CONN_HANDLE_INVALID)
            {
                err_code = ble_advertising_restart_without_whitelist();
                if (err_code != NRF_ERROR_INVALID_STATE)
                {
                    APP_ERROR_CHECK(err_code);
                }
            }
            break;

        default:
            break;
    }
}



/**@brief Function for the Power manager.
 */
static void power_manage(void)
{
    uint32_t err_code = sd_app_evt_wait();
    APP_ERROR_CHECK(err_code);
}


void reset_fifo()
{
   uint8_t const fifo_wrt_ptr[] = {0x04,0x00};
   uint8_t const fifo_rd_ptr[] = {0x05,0x00};//Mode configuration 
	 uint8_t const fifo_ovr_cnt[] = {0x06,0x00};//SPO2 configurarion
   
	 nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,fifo_wrt_ptr,sizeof(fifo_wrt_ptr),false);
   nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,fifo_ovr_cnt,sizeof(fifo_ovr_cnt),false);
   nrf_drv_twi_tx(&m_twi,MAX30100_ADDRESS,fifo_rd_ptr,sizeof(fifo_rd_ptr),false);

}




/**@brief Function for application main entry.
 */
int main(void)
{
   uint32_t err_code;
   bool erase_bonds = false;
	
	
	
   // Initialize.
	  nrf_gpio_cfg_output(12);
    nrf_gpio_pin_clear(12);
	
	  timers_init(); 
    ble_stack_init();
    device_manager_init(erase_bonds);
    gap_params_init();
    advertising_init();
    services_init();
    conn_params_init();  
	 
	 
    // Start execution.
    application_timers_start();
    err_code = ble_advertising_start(BLE_ADV_MODE_FAST);
  	APP_ERROR_CHECK(err_code);
    // Enter main loop.
    for (;;)
    {
			    power_manage();
			   

		  if(data_tx_ack_flag)
					{
							if(!over_write_flag)
							{	
								uint16_t no_samples=(m_sample_write_idx-m_sample_read_idx);
								uint8_t data_tx_ack[]={0x0B,no_samples>>8,no_samples,0xEE};	
								err_code = ble_hrs_heart_rate_measurement_send(&m_hrs,&data_tx_ack[0],4);
								if ((err_code != NRF_SUCCESS) &&
                    (err_code != NRF_ERROR_INVALID_STATE) &&
                    (err_code != BLE_ERROR_NO_TX_PACKETS) &&
                    (err_code != BLE_ERROR_GATTS_SYS_ATTR_MISSING)
                   )
                    {
                      APP_ERROR_HANDLER(err_code);
                    }
							}
						 else
							{
								uint16_t no_samples=((NUMBER_OF_SAMPLES-m_sample_read_idx)+(m_sample_read_idx));
								uint8_t data_tx_ack[]={0x0B,no_samples>>8,no_samples,0xEE};	
								err_code = ble_hrs_heart_rate_measurement_send(&m_hrs,&data_tx_ack[0],4);
							  if ((err_code != NRF_SUCCESS) &&
                    (err_code != NRF_ERROR_INVALID_STATE) &&
                    (err_code != BLE_ERROR_NO_TX_PACKETS) &&
                    (err_code != BLE_ERROR_GATTS_SYS_ATTR_MISSING)
                   )
                    {
                      APP_ERROR_HANDLER(err_code);
                    }
							}						 
            data_tx_ack_flag=false;	
          }
					
					
					
					if(no_data_flag)
					{
				  	uint8_t buffer_no_data_ack[]={0x0C,0x01,0x0A,0xEE};	
            err_code = ble_hrs_heart_rate_measurement_send(&m_hrs,&buffer_no_data_ack[0],4);		
            if ((err_code != NRF_SUCCESS) &&
                (err_code != NRF_ERROR_INVALID_STATE) &&
                (err_code != BLE_ERROR_NO_TX_PACKETS) &&
                (err_code != BLE_ERROR_GATTS_SYS_ATTR_MISSING)
               )
                {
                  APP_ERROR_HANDLER(err_code);
                }						
            no_data_flag=false;	
					}
					

			
			  if(epoch_time_receive)
				{
				  uint8_t epoch_time_ack[]={0XA1,0x01,0X0A,0XEE};
					err_code = ble_hrs_heart_rate_measurement_send(&m_hrs,&epoch_time_ack[0],4);
					if ((err_code != NRF_SUCCESS) &&
              (err_code != NRF_ERROR_INVALID_STATE) &&
              (err_code != BLE_ERROR_NO_TX_PACKETS) &&
              (err_code != BLE_ERROR_GATTS_SYS_ATTR_MISSING)
             )
                {
                  APP_ERROR_HANDLER(err_code);
                }
					epoch_time_receive=false;
			  }
				if(current_adv_rd_time_response)
				{
				  uint8_t current_adv_rd_time_interval[]={0XD1,(adv_interval>>8),adv_interval,(sensor_wakeup_time>>8),sensor_wakeup_time};
				  err_code = ble_hrs_heart_rate_measurement_send(&m_hrs,&current_adv_rd_time_interval[0],5);
					  if ((err_code != NRF_SUCCESS) &&
                (err_code != NRF_ERROR_INVALID_STATE) &&
                (err_code != BLE_ERROR_NO_TX_PACKETS) &&
                (err_code != BLE_ERROR_GATTS_SYS_ATTR_MISSING)
               )
                {
                  APP_ERROR_HANDLER(err_code);
                }
					current_adv_rd_time_response=false;
				}

         
       
		}		
    
}





