import { Component, computed, ElementRef, OnInit, signal, ViewChild, inject } from '@angular/core';
import { v4 as uuidv4 } from 'uuid';
import { PluginStatus, RestConstants, UIService } from '../../../core-module/core.module';
import { SharedModule } from 'src/app/shared/shared.module';
import { FormControl, FormGroup } from '@angular/forms';
import {
    AdminV1Service,
    ConfigService,
    ConfigV1Service,
    Context,
    RepositoryConfig,
    RepositoryMessage,
} from 'ngx-edu-sharing-api';
import { firstValueFrom } from 'rxjs';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { ContextNamePipe } from './context.directive';
import { MatChipEvent } from '@angular/material/chips';
import { EditorModule } from '@tinymce/tinymce-angular';
import { TranslateService } from '@ngx-translate/core';
import { PlatformLocation } from '@angular/common';
import { DELETE_OR_CANCEL } from '../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { DomSanitizer } from '@angular/platform-browser';
import { ShareDialogModule } from '../../../features/dialogs/dialog-modules/share-dialog/share-dialog.module';
import { MainNavService } from '../../../main/navigation/main-nav.service';
import { Toast } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-admin-messages',
    templateUrl: 'messages.component.html',
    styleUrls: ['messages.component.scss'],
    standalone: true,
    imports: [SharedModule, ContextNamePipe, EditorModule, ShareDialogModule],
})
export class AdminMessagesComponent implements OnInit {
    private configV1Service = inject(ConfigV1Service);
    private configService = inject(ConfigService);
    private uiService = inject(UIService);
    private platformLocation = inject(PlatformLocation);
    private mainNavService = inject(MainNavService);
    private translate = inject(TranslateService);
    private dialogs = inject(DialogsService);
    private toast = inject(Toast);
    private sanitizer = inject(DomSanitizer);
    private adminV1Service = inject(AdminV1Service);

    @ViewChild('heading') headingRef: ElementRef;
    selectedContexts = signal<Context[]>([]);
    contexts = signal<Context[]>([]);
    contextsFilter = signal('');
    availableContexts = computed(() =>
        this.contexts().filter(
            (c) =>
                c?.domain.join('').toLowerCase().includes(this.contextsFilter().toLowerCase()) &&
                !this.selectedContexts().find((f) => f.id === c.id),
        ),
    );
    selectedTp = signal<string[]>([]);
    tpFilter = signal('');
    componentFilter = signal('');
    tp = signal<string[]>([]);
    selectedComponents = signal<string[]>([]);
    components = signal<string[]>([]);
    availableTp = computed(() =>
        this.tp()
            .filter(
                (c) =>
                    c.toLowerCase().includes(this.tpFilter().toLowerCase()) &&
                    !this.selectedTp().find((f) => f === c),
            )
            .sort(),
    );
    availableComponents = computed(() =>
        this.components()
            .filter(
                (c) =>
                    this.translate
                        .instant('SIDEBAR.' + c.toUpperCase())
                        .toLowerCase()
                        .includes(this.componentFilter().toLowerCase()) &&
                    !this.selectedComponents().find((f) => f === c),
            )
            .sort(),
    );
    config = signal<RepositoryConfig>(null);

    fromDate = signal(new Date().getTime() + 1000 * 60 * 60 * 24);
    toDate = signal(new Date().getTime() + 1000 * 60 * 60 * 24);
    editId = signal<string>(null);
    plugins: PluginStatus[];
    createForm = new FormGroup({
        userMode: new FormControl('all'),
        mode: new FormControl('bar'),
        severity: new FormControl('info'),
        repeat: new FormControl('once'),
        fromEnabled: new FormControl(false),
        toEnabled: new FormControl(false),
    });
    editorConfig: any;
    message: string;

    async ngOnInit() {
        this.components.set(this.mainNavService.getAvailableScopes());
        this.config.set(await firstValueFrom(this.adminV1Service.getConfig()));
        this.tp.set(
            Object.keys(
                await firstValueFrom(
                    this.adminV1Service.getAllToolpermissions({
                        authority: RestConstants.AUTHORITY_EVERYONE,
                    }),
                ),
            ),
        );
        this.contexts.set(
            await firstValueFrom(this.configV1Service.getAvailableContext({ includeStatic: true })),
        );
        /*this.contexts.set([
            {
                id: 'test',
                domain: ['test.de', 'abc.de'],
            } as Context,
            {
                id: 'test2',
                domain: ['test2.de', 'abc.de'],
            } as Context,
        ]);*/
        if ((await this.configService.get<string>('admin.wysiwygType', 'TinyMCE')) === 'TinyMCE') {
            this.editorConfig = {
                base_url: this.platformLocation.getBaseHrefFromDOM() + 'assets/tinymce/',
                branding: false,
                height: 200,
                apiKey: '',
                menubar: false,
                statusbar: false,
                resize: true,
                //newline_behavior: 'linebreak',
                plugins: ['link', 'code'],
                toolbar:
                    'bold italic underline | link | alignleft aligncenter alignright alignjustify | removeformat | code | undo redo',
                language: this.translate.getDefaultLang(),
            };
        }
    }

    async addMessage() {
        const message = {
            message: this.message,
            userMode: this.createForm.get('userMode').value,
            mode: this.createForm.get('mode').value,
            severity: this.createForm.get('severity').value,
            repeat: this.createForm.get('repeat').value,
            components: this.selectedComponents(),
            contexts: this.selectedContexts().map((c) => c.id),
            from: this.createForm.get('fromEnabled').value ? this.fromDate() : null,
            to: this.createForm.get('toEnabled').value ? this.toDate() : null,
            toolpermissions: this.selectedTp(),
            uuid: uuidv4(),
        } as RepositoryMessage;
        if (message.from && message.to && message.to <= message.from) {
            this.toast.error(null, 'ADMIN.MESSAGES.INVALID_TO_DATE');
            return;
        }
        const config = this.config() || {};
        if (!config.messages) {
            config.messages = [];
        }
        if (this.editId()) {
            config.messages.splice(
                config.messages.findIndex((m) => m.uuid === this.editId()),
                1,
                message,
            );
        } else {
            config.messages.splice(0, 0, message);
        }
        await firstValueFrom(
            this.adminV1Service.setConfig({
                body: config,
            }),
        );
        this.config.set(config);
        this.reset();
    }

    reset() {
        this.createForm.setValue({
            userMode: 'all',
            severity: 'info',
            mode: 'bar',
            repeat: 'once',
            fromEnabled: false,
            toEnabled: false,
        });
        this.editId.set(null);
        this.message = '';
        this.selectedComponents.set([]);
        this.selectedContexts.set([]);
        this.selectedTp.set([]);
    }

    selectContext(event: MatAutocompleteSelectedEvent) {
        this.selectedContexts.set([...this.selectedContexts(), event.option.value]);
        this.contextsFilter.set('');
    }

    removeContext(event: MatChipEvent) {
        this.selectedContexts.set(
            this.selectedContexts().filter((c) => c.id !== event.chip.value.id),
        );
    }
    removeTp(event: MatChipEvent) {
        this.selectedTp.set(this.selectedTp().filter((c) => c !== event.chip.value));
    }
    removeComponent(event: MatChipEvent) {
        this.selectedComponents.set(
            this.selectedComponents().filter((c) => c !== event.chip.value),
        );
    }
    selectTp(event: MatAutocompleteSelectedEvent) {
        this.selectedTp.set([...this.selectedTp(), event.option.value]);
        this.tpFilter.set('');
    }
    selectComponent(event: MatAutocompleteSelectedEvent) {
        this.selectedComponents.set([...this.selectedComponents(), event.option.value]);
        this.componentFilter.set('');
    }

    editMessage(msg: RepositoryMessage) {
        this.createForm.setValue({
            userMode: msg.userMode,
            mode: msg.mode,
            repeat: msg.repeat,
            severity: msg.severity,
            fromEnabled: msg.from != null,
            toEnabled: msg.to != null,
        });
        this.message = msg.message;
        this.fromDate.set(msg.from);
        this.toDate.set(msg.to);
        this.selectedContexts.set(
            msg.contexts.map((c) => this.contexts().find((c2) => c2.id === c)),
        );
        this.selectedTp.set(msg.toolpermissions);
        this.selectedComponents.set(msg.components);
        this.editId.set(msg.uuid);
        void this.uiService.scrollSmooth(
            this.headingRef?.nativeElement?.getBoundingClientRect().top,
        );
    }
    async deleteMessage(msg: RepositoryMessage) {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'ADMIN.MESSAGES.REMOVE_TITLE',
            avatar: {
                kind: 'icon',
                icon: 'delete',
            },
            message: 'ADMIN.MESSAGES.REMOVE_MESSAGE',
            messageMode: 'html',
            messageParameters: { message: msg.message },
            buttons: DELETE_OR_CANCEL,
        });
        dialogRef.afterClosed().subscribe(async (result) => {
            if (result === 'YES_DELETE') {
                const config = this.config();
                if (!config.messages) {
                    config.messages = [];
                }
                config.messages = config.messages.filter((c) => c !== msg);
                await this.saveConfig(config);
            }
        });
    }
    getContext(id: string) {
        return this.contexts().find((c) => c.id === id);
    }
    private async saveConfig(config: RepositoryConfig) {
        await firstValueFrom(
            this.adminV1Service.setConfig({
                body: config,
            }),
        );
        this.config.set(config);
    }

    drop(event: CdkDragDrop<Array<RepositoryMessage>, any>) {
        const config = this.config();
        moveItemInArray(config.messages, event.previousIndex, event.currentIndex);
        void this.saveConfig(config);
    }
}
