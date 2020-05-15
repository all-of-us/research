import {SpinnerOverlay} from 'app/components/spinners';
import {withWindowSize} from 'app/utils';
import * as React from 'react';


export interface Props {
  containerStyles?: React.CSSProperties;
  onLastPageRender: () => void;
  filePath: string;
  windowSize: {
    width: number,
    height: number
  };
}

interface State {
  hasReadEntireDoc: boolean;
  loading: boolean;
}

export const HtmlViewer = withWindowSize()( class extends React.Component<Props,  State> {
  iframeRef: React.RefObject<any>;

  constructor(props) {
    super(props);

    this.state = {
      hasReadEntireDoc: false,
      loading: true
    };

    this.iframeRef = React.createRef();
  }

  private handleIframeLoaded() {
    const { onLastPageRender = () => false } = this.props;
    const iframeDocument = this.iframeRef.current.contentDocument;
    const { body } = iframeDocument;
    const openLinksInNewTab = iframeDocument.createElement('base');
    const endOfPage = iframeDocument.createElement('div');

    openLinksInNewTab.setAttribute('target', '_blank');
    body.prepend(openLinksInNewTab);
    body.appendChild(endOfPage);

    const observer = new IntersectionObserver(
      ([{ isIntersecting }]) => {
        if (isIntersecting && !this.state.hasReadEntireDoc) {
          onLastPageRender();
          this.setState({ hasReadEntireDoc: true });
        }
      },
      { root: null, threshold: 1.0 }
    );
    observer.observe(endOfPage);
    this.setState({ loading: false });
  }

  render() {
    const { loading } = this.state;
    const { filePath, containerStyles } = this.props;

    return <div style={{ flex: '1 1 0', position: 'relative', ...containerStyles }}>
      { loading && <SpinnerOverlay/> }
      <iframe
        style={{
          border: 'none',
          position: 'absolute',
          padding: '0 2rem',
          bottom: 0,
          top: 0,
          height: '100%',
          width: '100%'}}
          src = {filePath}
          ref = {this.iframeRef}
          onLoad={() => this.handleIframeLoaded() }>
      </iframe>
    </div>;
  }
});
