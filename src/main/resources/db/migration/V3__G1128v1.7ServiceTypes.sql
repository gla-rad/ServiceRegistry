-- Change the instance_service_type table names to plural
ALTER TABLE instance_service_type RENAME TO instance_service_types;
ALTER TABLE instance_service_types RENAME COLUMN service_type TO service_types;

-- Update the old values for the service types
UPDATE instance_service_types SET service_types = 'MS_1' WHERE service_types = 'VTSInformationService';
UPDATE instance_service_types SET service_types = 'MS_2' WHERE service_types = 'VTSNavigationalAssistanceService';
UPDATE instance_service_types SET service_types = 'MS_3' WHERE service_types = 'TrafficOrganizationService';
UPDATE instance_service_types SET service_types = 'MS_4' WHERE service_types = 'PortSupportService';
UPDATE instance_service_types SET service_types = 'MS_5' WHERE service_types = 'MaritimeSafetyInformationService';
UPDATE instance_service_types SET service_types = 'MS_6' WHERE service_types = 'PilotageService';
UPDATE instance_service_types SET service_types = 'MS_7' WHERE service_types = 'TugService';
UPDATE instance_service_types SET service_types = 'MS_8' WHERE service_types = 'VesselShoreReporting';
UPDATE instance_service_types SET service_types = 'MS_9' WHERE service_types = 'TelemedicalAssistanceService';
UPDATE instance_service_types SET service_types = 'MS_10' WHERE service_types = 'MaritimeAssistanceService';
UPDATE instance_service_types SET service_types = 'MS_11' WHERE service_types = 'NauticalChartService';
UPDATE instance_service_types SET service_types = 'MS_12' WHERE service_types = 'NauticalPublicationsService';
UPDATE instance_service_types SET service_types = 'MS_13' WHERE service_types = 'IceNavigationService';
UPDATE instance_service_types SET service_types = 'MS_14' WHERE service_types = 'MeteorologicalInformationService';
UPDATE instance_service_types SET service_types = 'MS_15' WHERE service_types = 'RealTimeHydrographicAndEnvironmentalInformationServices';
UPDATE instance_service_types SET service_types = 'MS_16' WHERE service_types = 'SearchAndRescueService';
UPDATE instance_service_types SET service_types = 'OTHER' WHERE service_types NOT LIKE 'MS%';