define(['settings'],
function (Settings) {
  return new Settings({
    //elasticsearch: "http://"+window.location.hostname+":9200",
    datasources: {
      influx: {
        default: true,
        grafanaDB: true,
        type: 'influxdb',
        // this need to be proxied by nginx
        url: "http://127.0.0.1:8086/db/samsara",
        username: "root",
        password: "root"
      }
    },
    default_route: '/dashboard/file/default.json',
    timezoneOffset: null,
    grafana_index: "grafana-dash",
    unsaved_changes_warning: true,
    panel_names: ['text','graphite']
  });
});
