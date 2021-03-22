import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { NativeWidget } from '../../mds-editor-view/mds-editor-view.component';
import { Values } from '../../types';
import { map } from 'rxjs/operators/map';

@Component({
    selector: 'app-mds-editor-widget-file-upload',
    templateUrl: './mds-editor-widget-file-upload.component.html',
    styleUrls: ['./mds-editor-widget-file-upload.component.scss'],
})
export class MdsEditorWidgetFileUploadComponent implements OnInit, NativeWidget {
    static readonly constraints = {
        requiresNode: false,
        supportsBulk: false,
    };
    selectedFiles = new BehaviorSubject<File[]>(null);
    hasChanges = new BehaviorSubject<boolean>(false);
    isFileOver = false;
    supportsDrop = true;
    link: string;
    status = this.selectedFiles.pipe(
        map((selectedFiles) => (selectedFiles?.length || this.link ? 'VALID' : 'INVALID')),
    );

    @Output() onSetLink = new EventEmitter<string>();

    constructor() {}

    ngOnInit(): void {}

    setLink() {
        this.onSetLink.emit(this.link);
    }

    setFilesByFileList(fileList: FileList) {
        if (this.link) {
            return;
        }
        const selectedFiles = [];
        for (let i = 0; i < fileList.length; i++) {
            selectedFiles.push(fileList.item(i));
        }
        this.selectedFiles.next(selectedFiles);
    }

    filesSelected(files: Event) {
        this.setFilesByFileList((files.target as HTMLInputElement).files);
    }

    async getValues(values: Values) {
        if (this.selectedFiles.value?.length) {
            const file = this.selectedFiles.value[0];
            const base64 = await new Promise((resolve, reject) => {
                const reader = new FileReader();
                reader.readAsDataURL(file);
                reader.onload = () => resolve(reader.result);
            });
            values['fileupload-filename'] = [file.name];
            values['fileupload-filetype'] = [file.type];
            values['fileupload-filedata'] = [base64 as string];
        } else {
            values['fileupload-link'] = [this.link];
        }
        return values;
    }
}
