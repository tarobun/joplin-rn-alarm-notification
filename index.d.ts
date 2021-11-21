export default ReactNativeAN;

declare namespace ReactNativeAN {
    function scheduleAlarm(details: any): Promise<any>;
    function deleteAlarm(id: any): void;
    function deleteRepeatingAlarm(id: any): void;
    function stopAlarmSound(): any;
    function removeFiredNotification(id: any): void;
    function removeAllFiredNotifications(): void;
    function getScheduledAlarms(): Promise<any>;
    function requestPermissions(permissions: any): Promise<any>;
    function checkPermissions(callback: any): void;
    function parseDate(rawDate: any): string;
}
