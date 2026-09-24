{{/* base url of the repository service - the port picks external (proxied traffic, 2FA applies) or internal (config) */}}
{{- define "edusharing_repository_frontend_server.backend.url" -}}
http://{{ .root.Values.config.backend.host }}:{{ .port }}
{{- end -}}
