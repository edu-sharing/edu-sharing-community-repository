{{/* base url of the repository service - the port picks external (proxied traffic, 2FA applies) or internal (config) */}}
{{- define "edusharing_repository_frontend_server.backend.url" -}}
http://{{ .root.Values.config.backend.host }}:{{ .port }}
{{- end -}}

{{/* base url of the elasticsearch index the shell metadata is read from */}}
{{- define "edusharing_repository_frontend_server.elastic.url" -}}
http://{{ .Values.config.elastic.host }}:{{ .Values.config.elastic.port }}
{{- end -}}
